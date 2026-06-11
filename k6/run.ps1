param(
    [string]$Dataset         = "default",
    [string]$Module          = "all",
    [int]   $TargetRps       = 100,
    [int]   $MaxVus          = 500,
    [string]$ApiProfile      = "ops\perf\profiles\api.env",
    [string]$DatasetProfile  = "ops\perf\profiles\dataset.env",
    [string]$HardwareProfile = "ops\perf\profiles\hardware.env",
    [string]$Gui             = "grafana",
    [switch]$KeepUp,
    [switch]$SkipInit,
    [switch]$Build,
    [switch]$Clean,
    [switch]$Debug
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
<#
.SYNOPSIS
  HelloCS 부하테스트를 실행합니다.
  ops/perf/profiles/*.env 에서 API 비율·SLO, 하드웨어 한도를 읽어 구동합니다.

.EXAMPLE
  .\k6\run.ps1                                        # 기본값 실행 (TargetRps=100, Module=all)
  .\k6\run.ps1 -TargetRps 200                         # 전체 믹스 200 RPS
  .\k6\run.ps1 -Module grading -TargetRps 80          # grading 모듈만 (비율 그대로 유지)
  .\k6\run.ps1 -HardwareProfile ops\perf\profiles\hw-v2.env  # step 3b: 하드웨어 재할당
  .\k6\run.ps1 -Dataset default -SkipInit             # 재실행 (스택 초기화 생략)

.PARAMETER Dataset
  사용할 데이터셋 이름 (ops/perf/datasets/<Name>). 먼저 bake-dataset.ps1 로 생성해야 합니다.

.PARAMETER Module
  실행할 모듈 필터: all | streak | ranking | quiz | grading | user | topic (기본: all)
  step 2 모듈별 최적화 시 사용. 비율·RPS 는 api.env 그대로 유지됨.

.PARAMETER TargetRps
  전체 믹스의 목표 최대 RPS. 각 엔드포인트 RPS = TargetRps × (RATIO / 100) (기본: 100)

.PARAMETER MaxVus
  VU 상한 (기본: 500)

.PARAMETER ApiProfile
  API 비율·SLO 정의 파일 (기본: ops/perf/profiles/api.env)

.PARAMETER DatasetProfile
  데이터셋 세그먼트·토큰풀 정의 파일 (기본: ops/perf/profiles/dataset.env)

.PARAMETER HardwareProfile
  서비스별 CPU/메모리 한도 정의 파일 (기본: ops/perf/profiles/hardware.env)
  docker-compose 의 POSTGRES_QUIZ_CPU_LIMIT 등 변수를 공급. 뒤 env-file 이 우선 적용됨.

.PARAMETER Gui
  "grafana" (기본) | "web" (k6 내장 대시보드) | "none" (CI 모드)

.PARAMETER KeepUp
  테스트 종료 후 스택을 내리지 않습니다.

.PARAMETER SkipInit
  스택 초기화(down -v → up)를 건너뜁니다. 코드 변경이 없고 재실행만 할 때 사용하세요.

.PARAMETER Build
  Gradle bootJar 로 jar를 빌드한 뒤 이미지를 재빌드해 실행합니다.

.PARAMETER Clean
  Prometheus·Grafana 의 수집 데이터를 초기화합니다.

.PARAMETER Debug
  앱 컨테이너 로그를 출력합니다.
#>

$COMPOSE_FILE    = "docker-compose.perf.yml"
$ENV_FILE        = ".env.perf"
$PROFILE_JSON    = "k6\.perf-profile.json"
$HEALTH_URL      = "http://localhost:8081/actuator/health/readiness"
$WIREMOCK_URL    = "http://localhost:8089/__admin/health"
$STARTUP_TIMEOUT = 90

$VALID_MODULES = @("all","streak","ranking","quiz","grading","user","topic")
if ($Module -notin $VALID_MODULES) {
    Write-Host "Module 값이 올바르지 않습니다: $Module (가능: $($VALID_MODULES -join ' / '))" -ForegroundColor Red
    exit 1
}

$TIMESTAMP    = Get-Date -Format "yyyyMMdd_HHmmss"
$RESULTS_DIR  = "k6\results"
$SUMMARY_FILE = "$RESULTS_DIR\$TIMESTAMP-summary.json"

New-Item -ItemType Directory -Force -Path $RESULTS_DIR | Out-Null

# ── 데이터셋 확인 ────────────────────────────────────────────────
$datasetDir  = "ops\perf\datasets\$Dataset"
$datasetMeta = "$datasetDir\meta.json"
if (-not (Test-Path $datasetDir)) {
    Write-Host ""
    Write-Host "데이터셋 '$Dataset' 을 찾을 수 없습니다." -ForegroundColor Red
    Write-Host "먼저 실행하세요: .\k6\bake-dataset.ps1 -Name $Dataset" -ForegroundColor Yellow
    exit 1
}

# ── .env 파서 ────────────────────────────────────────────────────
function Read-EnvFile($path) {
    $map = [ordered]@{}
    if (-not (Test-Path $path)) { return $map }
    Get-Content $path | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$' -and $_ -notmatch '^\s*#') {
            $map[$Matches[1]] = ($Matches[2] -replace '\s*#.*$', '').Trim()
        }
    }
    return $map
}

# ── profile.json 생성 ────────────────────────────────────────────
Write-Host ""
Write-Host "=== HelloCS 부하테스트 ===" -ForegroundColor Cyan
Write-Host "  Dataset: $Dataset  Module: $Module  TargetRps: $TargetRps" -ForegroundColor DarkGray

$api  = Read-EnvFile $ApiProfile
$ds   = Read-EnvFile $DatasetProfile

# 엔드포인트 이름 목록 (api.env의 RATIO_* 키에서 접두사 제거)
$EP_NAMES = @(
    "STREAK_SUMMARY","STREAK_MONTHLY","STREAK_DETAIL",
    "RANKING_SUMMARY","RANKING_PAGE",
    "QUIZ_FETCH",
    "GRADING_SUBMIT","GRADING_RESULT","GRADING_DETAIL","GRADING_LIST",
    "USER_ME","TOPICS"
)

$endpointsMap = [ordered]@{}
foreach ($ep in $EP_NAMES) {
    $ratio = if ($api.Contains("RATIO_$ep")) { [int]$api["RATIO_$ep"] } else { 0 }
    $p95Raw = if ($api.Contains("P95_$ep"))  { $api["P95_$ep"]         } else { "" }
    $p95    = if ($p95Raw -match '^\d+$')       { [int]$p95Raw            } else { $null }
    $endpointsMap[$ep] = [ordered]@{ ratio = $ratio; p95 = $p95 }
}

$tokenPool = [ordered]@{
    size     = if ($ds["TOKEN_POOL_SIZE"])    { [int]$ds["TOKEN_POOL_SIZE"]    } else { 200 }
    wPower   = if ($ds["TOKEN_POOL_W_POWER"]) { [double]$ds["TOKEN_POOL_W_POWER"]   } else { 0.30 }
    wRegular = if ($ds["TOKEN_POOL_W_REGULAR"]) { [double]$ds["TOKEN_POOL_W_REGULAR"] } else { 0.50 }
    wCasual  = if ($ds["TOKEN_POOL_W_CASUAL"])  { [double]$ds["TOKEN_POOL_W_CASUAL"]  } else { 0.20 }
}

$dataset = [ordered]@{
    users          = if ($ds["DATASET_USERS"])      { [int]$ds["DATASET_USERS"]          } else { 10000 }
    segPowerShare  = if ($ds["SEG_POWER_SHARE"])    { [double]$ds["SEG_POWER_SHARE"]     } else { 0.2 }
    segRegularShare = if ($ds["SEG_REGULAR_SHARE"]) { [double]$ds["SEG_REGULAR_SHARE"]   } else { 0.5 }
    segCasualShare = if ($ds["SEG_CASUAL_SHARE"])   { [double]$ds["SEG_CASUAL_SHARE"]    } else { 0.3 }
}

$profileObj = [ordered]@{
    endpoints = $endpointsMap
    tokenPool = $tokenPool
    dataset   = $dataset
}
$profileObj | ConvertTo-Json -Depth 4 | Out-File -FilePath $PROFILE_JSON -Encoding utf8 -NoNewline
Write-Host "  Profile JSON: $PROFILE_JSON 생성 완료" -ForegroundColor DarkGray

# ── meta.json 에서 유저/일수 읽기 ────────────────────────────────
$seedUserCount = $dataset.users
$seedDays      = 30
if (Test-Path $datasetMeta) {
    $meta          = Get-Content $datasetMeta | ConvertFrom-Json
    $seedUserCount = $meta.userCount
    $seedDays      = $meta.days
    Write-Host "  Dataset meta: users=$seedUserCount, days=$seedDays" -ForegroundColor DarkGray
}

# ── compose 공통 인수 ─────────────────────────────────────────────
# hardware.env 가 뒤 env-file 이므로 .env.perf 의 공유 변수를 덮어씀.
$composeBase = @(
    "docker", "compose", "-p", "hellocs-perf",
    "--env-file", $ENV_FILE,
    "--env-file", $HardwareProfile,
    "-f", $COMPOSE_FILE
)

function Invoke-Compose([string[]]$args) {
    wsl @composeBase @args
}

function Test-UrlReady($url) {
    try {
        $r = Invoke-WebRequest -Uri $url -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

# ── [1/3] 스택 초기화 ────────────────────────────────────────────
Write-Host ""
if ($Clean) {
    Write-Host "[1/3] 모니터링 데이터 초기화 중 (Prometheus·Grafana 볼륨 삭제) ..." -ForegroundColor Yellow
    Invoke-Compose @("--profile", "monitoring", "down", "-v") 2>&1 | Out-Null
}

if ($SkipInit) {
    Write-Host "[1/3] 스택 초기화 건너뜀 (-SkipInit)" -ForegroundColor DarkGray
    wsl env "DATASET=$Dataset" @composeBase --profile app --profile monitoring up -d
} elseif ($Build) {
    Write-Host "[1/3] 스택 초기화 중 (bootJar → down -v → up --build) ..." -ForegroundColor Yellow
    Write-Host "      Gradle bootJar 빌드 중 ..." -ForegroundColor DarkGray
    & .\gradlew.bat bootJar -x test
    if ($LASTEXITCODE -ne 0) {
        Write-Host "      Gradle bootJar 실패. 중단합니다." -ForegroundColor Red
        exit 1
    }
    Invoke-Compose @("--profile", "app", "down", "-v") 2>&1 | Out-Null
    wsl env "DATASET=$Dataset" @composeBase --profile app --profile monitoring up -d --build
} else {
    Write-Host "[1/3] 스택 초기화 중 (down -v → up) ..." -ForegroundColor Yellow
    Invoke-Compose @("--profile", "app", "down", "-v") 2>&1 | Out-Null
    wsl env "DATASET=$Dataset" @composeBase --profile app --profile monitoring up -d
}

# WireMock 준비 대기
$infraReady = $false
for ($i = 0; $i -lt 20; $i++) {
    if (Test-UrlReady $WIREMOCK_URL) { $infraReady = $true; break }
    Start-Sleep -Seconds 3
    Write-Host "     WireMock 대기 중 ($([int](($i+1)*3))s / 60s) ..." -ForegroundColor DarkGray
}
if (-not $infraReady) {
    Write-Host "     WireMock 응답 없음. 로그를 확인하세요." -ForegroundColor Red
    Invoke-Compose @("logs", "wiremock")
    if (-not $KeepUp) { Invoke-Compose @("--profile", "app", "down") 2>&1 | Out-Null }
    exit 1
}

# cadvisor 준비 대기
$cadvisorReady = $false
for ($i = 0; $i -lt 10; $i++) {
    if (Test-UrlReady "http://localhost:9080/healthz") { $cadvisorReady = $true; break }
    Start-Sleep -Seconds 3
    Write-Host "     cadvisor 대기 중 ($([int](($i+1)*3))s / 30s) ..." -ForegroundColor DarkGray
}
if ($cadvisorReady) {
    Write-Host "     cadvisor 준비 완료" -ForegroundColor Green
} else {
    Write-Host "     cadvisor 응답 없음 — Container CFS Throttle 패널이 비어있을 수 있습니다. 계속 진행합니다." -ForegroundColor Yellow
}

# ── [2/3] 앱 준비 대기 ───────────────────────────────────────────
Write-Host ""
Write-Host "[2/3] 앱 준비 대기 중 (최대 ${STARTUP_TIMEOUT}s) ..." -ForegroundColor Yellow

$elapsed  = 0
$appReady = $false
while ($elapsed -lt $STARTUP_TIMEOUT) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    if (Test-UrlReady $HEALTH_URL) { $appReady = $true; break }
    if ($Debug) {
        Invoke-Compose @("logs", "--since", "5s", "gateway") 2>&1 |
            ForEach-Object { Write-Host "  > $_" -ForegroundColor DarkGray }
    }
    Write-Host "     ...${elapsed}s / ${STARTUP_TIMEOUT}s" -ForegroundColor DarkGray
}

if (-not $appReady) {
    Write-Host "     앱 시작 실패. 로그를 확인하세요." -ForegroundColor Red
    Invoke-Compose @("logs", "gateway")
    if (-not $KeepUp) { Invoke-Compose @("--profile", "app", "down") 2>&1 | Out-Null }
    exit 1
}

try {
    Invoke-WebRequest -Uri "http://127.0.0.1:9090/-/reload" -Method POST -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop | Out-Null
    Write-Host "     앱 준비 완료 (Prometheus 리로드 완료)" -ForegroundColor Green
} catch {
    Write-Host "     앱 준비 완료 (Prometheus 리로드 실패 — 무시하고 계속)" -ForegroundColor Yellow
}

# ── [3/3] k6 실행 ────────────────────────────────────────────────
Write-Host ""
Write-Host "[3/3] k6 시작..." -ForegroundColor Yellow
Write-Host "     모듈        : $Module"
Write-Host "     목표 RPS    : $TargetRps  (엔드포인트별 = TargetRps × RATIO/100)"
Write-Host "     결과 저장   : $SUMMARY_FILE"
Write-Host ""

$k6Args = @(
    "run",
    "-e", "TARGET_RPS=$TargetRps",
    "-e", "MODULE=$Module",
    "-e", "MAX_VUS=$MaxVus",
    "-e", "TOKEN_POOL_SIZE=$($tokenPool.size)",
    "-e", "DATASET_USERS=$($dataset.users)",
    "-e", "SEED_USER_COUNT=$seedUserCount",
    "-e", "SEED_DAYS=$seedDays",
    "-e", "SKIP_SEED=true",
    "-e", "DEBUG=$($Debug.IsPresent)",
    "-e", "RESULTS_FILE=$SUMMARY_FILE"
)

$env:K6_PROMETHEUS_RW_SERVER_URL                = "http://127.0.0.1:9090/api/v1/write"
$env:K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM  = "true"
$k6Args += "--out"; $k6Args += "experimental-prometheus-rw"

if ($Gui -eq "web") {
    $env:K6_WEB_DASHBOARD      = "true"
    $env:K6_WEB_DASHBOARD_OPEN = "true"
    Write-Host "     k6 Dashboard : http://localhost:5665" -ForegroundColor Green
    Write-Host "     Grafana       : http://localhost:3000  (admin / admin)" -ForegroundColor Green
} elseif ($Gui -eq "grafana") {
    Write-Host "     Grafana       : http://localhost:3000  (admin / admin)" -ForegroundColor Green
}

try {
    k6 @k6Args "k6/all.js"
} finally {
    if (-not $KeepUp) {
        Write-Host ""
        Write-Host "테스트 완료. 앱/DB 종료 중... (Prometheus/Grafana/cadvisor/node-exporter 유지)" -ForegroundColor Cyan
        Invoke-Compose @("--profile", "app", "down") 2>&1 | Out-Null
    } else {
        Write-Host ""
        Write-Host "스택 유지 중 (-KeepUp 플래그). 종료하려면:" -ForegroundColor Cyan
        Write-Host "  wsl docker compose -p hellocs-perf --env-file $ENV_FILE --env-file $HardwareProfile -f $COMPOSE_FILE --profile app down" -ForegroundColor DarkGray
        Write-Host "  (monitoring 포함: ... --profile app --profile monitoring down)" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "─────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "  AI 분석을 원하면 Claude Code 에서:" -ForegroundColor Cyan
Write-Host "  /analyze" -ForegroundColor Yellow
Write-Host "─────────────────────────────────────────" -ForegroundColor DarkGray
