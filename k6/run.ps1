param(
    [string]$Dataset         = "default",
    [string]$Module          = "all",
    [int]   $StartRps        = 10,
    [int]   $EndRps          = 100,
    [int]   $StepRps         = 10,
    [string]$StepDuration    = "2m",
    [int]   $MaxVus          = 500,
    [string]$ApiProfile      = "ops/perf/profiles/api.env",
    [string]$DatasetProfile  = "ops/perf/profiles/dataset.env",
    [string]$HardwareProfile = "ops/perf/profiles/hardware.env",
    [string]$Gui             = "grafana",
    [string]$TargetHost     = $(if ($env:PERF_TARGET_HOST) { $env:PERF_TARGET_HOST } else { "localhost" }),
    [switch]$KeepUp,
    [switch]$SkipInit,
    [switch]$Build,
    [switch]$Clean,
    [switch]$VerifyOnly,
    [switch]$Debug
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
<#
.SYNOPSIS
  HelloCS 부하테스트를 실행합니다.
  ops/perf/profiles/*.env 에서 API 비율·SLO, 하드웨어 한도를 읽어 구동합니다.

.EXAMPLE
  .\k6\run.ps1                                                         # 기본값 (10→100, step 10, 2m/step)
  .\k6\run.ps1 -StartRps 50 -EndRps 500 -StepRps 50                   # 50→500 RPS 탐색
  .\k6\run.ps1 -StartRps 10 -EndRps 200 -StepRps 10 -StepDuration 3m  # 스텝당 3분 유지
  .\k6\run.ps1 -Module grading -EndRps 200                             # grading 모듈만
  .\k6\run.ps1 -HardwareProfile ops\perf\profiles\hw-v2.env            # step 3b: 하드웨어 재할당
  .\k6\run.ps1 -Dataset default -SkipInit                              # 재실행 (스택 초기화 생략)

.PARAMETER Dataset
  사용할 데이터셋 이름 (ops/perf/datasets/<Name>). 먼저 bake-dataset.ps1 로 생성해야 합니다.

.PARAMETER Module
  실행할 모듈 필터: all | streak | ranking | quiz | grading | user | topic (기본: all)
  단일 모듈 지정 시 — 격리 모드 진입:
    1) 해당 모듈 + 전용 DB(postgres-quiz/user/topic 중 해당)에만 hardware.env 한도 적용, 나머지 0(무제한).
       단, 공유 스토어(Redis/Mongo/RabbitMQ)를 쓰는 grading/streak/ranking 은 DB 제외 — 모듈 컨테이너만 한도 적용.
    2) 해당 모듈 엔드포인트 비율을 합이 100이 되도록 재정규화 → START_RPS~END_RPS 가 해당 모듈 전체 RPS.
  'all' 지정 시 기존 동작 유지 (모든 서비스 한도 적용, api.env 비율 그대로).

.PARAMETER StartRps
  시작 RPS (기본: 10)

.PARAMETER EndRps
  최대 RPS (기본: 100). 의도적으로 크게 잡아 한계점을 탐색.

.PARAMETER StepRps
  스텝 단위 RPS (기본: 10). StartRps → EndRps 를 이 단위로 계단식 증가.

.PARAMETER StepDuration
  각 스텝에서 해당 RPS 를 유지하는 시간 (기본: "2m").

.PARAMETER MaxVus
  VU 상한 (기본: 500)

.PARAMETER ApiProfile
  API 비율·SLO 정의 파일 (기본: ops/perf/profiles/api.env)

.PARAMETER DatasetProfile
  데이터셋 세그먼트·토큰풀 정의 파일 (기본: ops/perf/profiles/dataset.env)

.PARAMETER HardwareProfile
  서비스별 CPU/메모리 한도 정의 파일 (기본: ops/perf/profiles/hardware.env)
  -Module all: 이 파일을 그대로 docker compose 에 전달.
  -Module <단일>: 런타임에 k6/.hardware-effective.env 를 생성. 대상 모듈+전용 DB 외 모든
  *_CPU_LIMIT/*_MEMORY_LIMIT 을 0(무제한)으로 재정의 후 이 파일 대신 사용.

.PARAMETER Gui
  "grafana" (기본) | "web" (k6 내장 대시보드) | "none" (CI 모드)

.PARAMETER TargetHost
  대상 스택 호스트. 외부 서버 테스트 시 서버 IP 지정 (기본: localhost, 또는 $env:PERF_TARGET_HOST). k6·헬스체크·Prometheus remote-write 가 이 호스트로 향함.

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
$PROFILE_JSON    = "k6/.perf-profile.json"
$HEALTH_URL      = "http://${TargetHost}:8081/actuator/health/readiness"
$WIREMOCK_URL    = "http://${TargetHost}:8089/__admin/health"

$VALID_MODULES = @("all","streak","ranking","quiz","grading","user","topic")
if ($Module -notin $VALID_MODULES) {
    Write-Host "Module 값이 올바르지 않습니다: $Module (가능: $($VALID_MODULES -join ' / '))" -ForegroundColor Red
    exit 1
}

$TIMESTAMP    = Get-Date -Format "yyyyMMdd_HHmmss"
$RESULTS_DIR  = "k6/results"
$SUMMARY_FILE = "$RESULTS_DIR/$TIMESTAMP-$Module-summary.json"

New-Item -ItemType Directory -Force -Path $RESULTS_DIR | Out-Null

# ── 데이터셋 확인 ────────────────────────────────────────────────
$datasetDir  = "ops/perf/datasets/$Dataset"
$datasetMeta = "$datasetDir/meta.json"
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
Write-Host "  Dataset: $Dataset  Module: $Module  RPS: $StartRps → $EndRps (step $StepRps, ${StepDuration}/step)" -ForegroundColor DarkGray
Write-Host "  TargetHost: $TargetHost" -ForegroundColor DarkGray

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

# ── 단일 모듈 격리 — 비율 재정규화 ─────────────────────────────────
# 단일 모듈 지정 시 해당 모듈 엔드포인트 비율을 합이 100이 되도록 재정규화.
# all.js 는 ratio=0 인 엔드포인트를 건너뜀 → 해당 모듈만 START_RPS~END_RPS 전체 사용.
if ($Module -ne 'all') {
    $EP_MODULE = @{
        STREAK_SUMMARY  = "streak";  STREAK_MONTHLY  = "streak";  STREAK_DETAIL   = "streak"
        RANKING_SUMMARY = "ranking"; RANKING_PAGE    = "ranking"
        QUIZ_FETCH      = "quiz"
        GRADING_SUBMIT  = "grading"; GRADING_RESULT  = "grading"; GRADING_DETAIL  = "grading"; GRADING_LIST = "grading"
        USER_ME         = "user"
        TOPICS          = "topic"
    }
    $moduleRatioSum = 0.0
    foreach ($ep in $EP_NAMES) {
        if ($EP_MODULE[$ep] -eq $Module) { $moduleRatioSum += $endpointsMap[$ep].ratio }
    }
    foreach ($ep in $EP_NAMES) {
        if ($EP_MODULE[$ep] -eq $Module -and $moduleRatioSum -gt 0) {
            $endpointsMap[$ep].ratio = [math]::Round($endpointsMap[$ep].ratio * 100.0 / $moduleRatioSum, 1)
        } else {
            $endpointsMap[$ep].ratio = 0
        }
    }
}

$tokenPool = [ordered]@{
    size     = if ($ds["TOKEN_POOL_SIZE"])    { [int]$ds["TOKEN_POOL_SIZE"]    } else { 200 }
    wPower   = if ($ds["TOKEN_POOL_W_POWER"]) { [double]$ds["TOKEN_POOL_W_POWER"]   } else { 0.30 }
    wRegular = if ($ds["TOKEN_POOL_W_REGULAR"]) { [double]$ds["TOKEN_POOL_W_REGULAR"] } else { 0.50 }
    wCasual  = if ($ds["TOKEN_POOL_W_CASUAL"])  { [double]$ds["TOKEN_POOL_W_CASUAL"]  } else { 0.20 }
}

$datasetCfg = [ordered]@{
    users          = if ($ds["DATASET_USERS"])      { [int]$ds["DATASET_USERS"]          } else { 10000 }
    segPowerShare  = if ($ds["SEG_POWER_SHARE"])    { [double]$ds["SEG_POWER_SHARE"]     } else { 0.2 }
    segRegularShare = if ($ds["SEG_REGULAR_SHARE"]) { [double]$ds["SEG_REGULAR_SHARE"]   } else { 0.5 }
    segCasualShare = if ($ds["SEG_CASUAL_SHARE"])   { [double]$ds["SEG_CASUAL_SHARE"]    } else { 0.3 }
}

# ── meta.json 에서 유저/일수 읽기 ────────────────────────────────
$seedUserCount = $datasetCfg.users
$seedDays      = 30
if (Test-Path $datasetMeta) {
    $meta          = Get-Content $datasetMeta | ConvertFrom-Json
    $seedUserCount = $meta.users
    $seedDays      = $meta.signupWindowDays
    Write-Host "  Dataset meta: users=$seedUserCount, days=$seedDays" -ForegroundColor DarkGray
}

# 시드 유저 수를 초과하는 토큰 풀은 무의미 — 초과분은 미존재 유저(kakaoId)라 인증 실패(user_me 401 등) 유발.
# TOKEN_POOL_SIZE 를 실제 시드 유저 수로 상한. (예: minimum=50 인데 dataset.env=1000)
if ($tokenPool.size -gt $seedUserCount) {
    Write-Host "  토큰 풀 상한: $($tokenPool.size) → $seedUserCount (시드 유저 수)" -ForegroundColor Yellow
    $tokenPool.size = $seedUserCount
}

$profileObj = [ordered]@{
    endpoints = $endpointsMap
    tokenPool = $tokenPool
    dataset   = $datasetCfg
}
$profileObj | ConvertTo-Json -Depth 4 | Out-File -FilePath $PROFILE_JSON -Encoding utf8 -NoNewline
Write-Host "  Profile JSON: $PROFILE_JSON 생성 완료" -ForegroundColor DarkGray

# ── 하드웨어 격리 env 파일 생성 ──────────────────────────────────────
# 단일 모듈 지정 시 대상 모듈 + 전용 DB 외 모든 *_CPU_LIMIT/*_MEMORY_LIMIT 을 0(무제한)으로 재정의.
# 공유 스토어(Redis/Mongo/RabbitMQ) 모듈(grading/streak/ranking)은 DB 제외 — 모듈 컨테이너만 한도 적용.
$EFFECTIVE_HW_FILE = "k6/.hardware-effective.env"
$MODULE_KEEP_VARS = @{
    quiz    = @("QUIZ_CPU_LIMIT","QUIZ_MEMORY_LIMIT","POSTGRES_QUIZ_CPU_LIMIT","POSTGRES_QUIZ_MEMORY_LIMIT")
    user    = @("USER_CPU_LIMIT","USER_MEMORY_LIMIT","POSTGRES_USER_CPU_LIMIT","POSTGRES_USER_MEMORY_LIMIT")
    topic   = @("TOPIC_CPU_LIMIT","TOPIC_MEMORY_LIMIT","POSTGRES_TOPIC_CPU_LIMIT","POSTGRES_TOPIC_MEMORY_LIMIT")
    grading = @("GRADING_CPU_LIMIT","GRADING_MEMORY_LIMIT")
    streak  = @("STREAK_CPU_LIMIT","STREAK_MEMORY_LIMIT")
    ranking = @("RANKING_CPU_LIMIT","RANKING_MEMORY_LIMIT")
}
if ($Module -eq 'all') {
    $effectiveHwFile = $HardwareProfile
} else {
    $keepVars = $MODULE_KEEP_VARS[$Module]
    $hwEnv    = Read-EnvFile $HardwareProfile
    $hwLines  = @()
    foreach ($key in $hwEnv.Keys) {
        if ($key -match '_CPU_LIMIT$|_MEMORY_LIMIT$') {
            if ($keepVars -contains $key) {
                $hwLines += "$key=$($hwEnv[$key])"
            } else {
                $hwLines += "$key=0"
            }
        } else {
            $hwLines += "$key=$($hwEnv[$key])"
        }
    }
    ($hwLines -join "`n") | Out-File -FilePath $EFFECTIVE_HW_FILE -Encoding ascii -NoNewline
    Write-Host "  격리 모드: [$Module] 모듈 + 전용 DB 에만 리소스 한도 적용, 나머지 0(무제한)" -ForegroundColor DarkGray
    $effectiveHwFile = $EFFECTIVE_HW_FILE
}

# ── compose 공통 인수 ─────────────────────────────────────────────
# $effectiveHwFile 이 뒤 env-file 이므로 .env.perf 의 공유 변수를 덮어씀.
# -Module all: hardware.env 직접 사용. -Module <단일>: k6/.hardware-effective.env 사용.
$composeBase = @(
    "docker", "compose", "-p", "hellocs-perf",
    "--env-file", $ENV_FILE,
    "--env-file", $effectiveHwFile,
    "-f", $COMPOSE_FILE
)

function Invoke-Compose([string[]]$cmdArgs) {
    wsl @composeBase @cmdArgs
}

function Test-UrlReady($url) {
    try {
        $r = Invoke-WebRequest -Uri $url -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

# 동적 데이터셋 외부 볼륨 보장 (원격 데몬 상주) — bake 가 채운 hellocs-perf-datasets 를 읽는다.
wsl docker volume create hellocs-perf-datasets | Out-Null

# 모니터링·postgres init 설정 볼륨 보장 — ops/{prometheus,tempo,grafana,wiremock,postgres_exporter,postgres} 를
# 원격 상주 named volume(hellocs-perf-config)에 push. bind mount 대신 사용.
# (postgres/init 은 postgres 컨테이너의 /docker-entrypoint-initdb.d subpath 마운트로 데이터셋 복원에 사용)
wsl docker volume create hellocs-perf-config | Out-Null
$repoRoot = (Get-Location).Path
$opsWsl   = '/mnt/' + $repoRoot[0].ToString().ToLower() + ($repoRoot.Substring(2) -replace '\\', '/') + '/ops'
wsl bash -c "tar -C '$opsWsl' -czf - prometheus tempo grafana wiremock postgres_exporter postgres | docker run --rm -i -v hellocs-perf-config:/perf-config alpine tar -C /perf-config -xzf -"

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
    if (Test-UrlReady "http://${TargetHost}:9080/healthz") { $cadvisorReady = $true; break }
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
Write-Host "[2/3] 앱 준비 대기 중 (Ctrl+C 로 중단) ..." -ForegroundColor Yellow

$elapsed = 0
while ($true) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    if (Test-UrlReady $HEALTH_URL) { break }
    if ($Debug) {
        Invoke-Compose @("logs", "--since=5s", "gateway") 2>&1 |
            ForEach-Object { Write-Host "  > $_" -ForegroundColor DarkGray }
    }
    Write-Host "     ...${elapsed}s" -ForegroundColor DarkGray
}

try {
    Invoke-WebRequest -Uri "http://${TargetHost}:9090/-/reload" -Method POST -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop | Out-Null
    Write-Host "     앱 준비 완료 (Prometheus 리로드 완료)" -ForegroundColor Green
} catch {
    Write-Host "     앱 준비 완료 (Prometheus 리로드 실패 — 무시하고 계속)" -ForegroundColor Yellow
}

# ── [3/3] k6 실행 ────────────────────────────────────────────────
Write-Host ""
Write-Host "[3/3] k6 시작..." -ForegroundColor Yellow
Write-Host "     모듈        : $Module"
Write-Host "     RPS 범위    : $StartRps → $EndRps  (step $StepRps, ${StepDuration}/step)"
Write-Host "     결과 저장   : $SUMMARY_FILE"
Write-Host ""

$k6Args = @(
    "run",
    "-e", "START_RPS=$StartRps",
    "-e", "END_RPS=$EndRps",
    "-e", "STEP_RPS=$StepRps",
    "-e", "STEP_DURATION=$StepDuration",
    "-e", "MODULE=$Module",
    "-e", "MAX_VUS=$MaxVus",
    "-e", "TOKEN_POOL_SIZE=$($tokenPool.size)",
    "-e", "DATASET_USERS=$($datasetCfg.users)",
    "-e", "SEED_USER_COUNT=$seedUserCount",
    "-e", "SEED_DAYS=$seedDays",
    "-e", "SKIP_SEED=true",
    "-e", "VERIFY_ONLY=$($VerifyOnly.IsPresent)",
    "-e", "DEBUG=$($Debug.IsPresent)",
    "-e", "RESULTS_FILE=$SUMMARY_FILE",
    "-e", "BASE_URL=http://${TargetHost}:8080",
    "-e", "WIREMOCK_URL=http://${TargetHost}:8089"
)

$env:K6_PROMETHEUS_RW_SERVER_URL                = "http://${TargetHost}:9090/api/v1/write"
$env:K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM  = "true"
$k6Args += "--out"; $k6Args += "experimental-prometheus-rw"

if ($Gui -eq "web") {
    $env:K6_WEB_DASHBOARD      = "true"
    $env:K6_WEB_DASHBOARD_OPEN = "true"
    Write-Host "     k6 Dashboard : http://localhost:5665" -ForegroundColor Green
    Write-Host "     Grafana       : http://${TargetHost}:3000  (admin / admin)" -ForegroundColor Green
} elseif ($Gui -eq "grafana") {
    Write-Host "     Grafana       : http://${TargetHost}:3000  (admin / admin)" -ForegroundColor Green
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
        Write-Host "  wsl docker compose -p hellocs-perf --env-file $ENV_FILE --env-file $effectiveHwFile -f $COMPOSE_FILE --profile app down" -ForegroundColor DarkGray
        Write-Host "  (monitoring 포함: ... --profile app --profile monitoring down)" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "─────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "  AI 분석을 원하면 Claude Code 에서:" -ForegroundColor Cyan
Write-Host "  /analyze" -ForegroundColor Yellow
Write-Host "─────────────────────────────────────────" -ForegroundColor DarkGray
