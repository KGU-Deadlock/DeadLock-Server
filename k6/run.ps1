param(
    [string]$Dataset    = "default",
    [string]$Scenario   = "all",
    [int]$RpsRanking    = 200,
    [int]$RpsStreak     = 100,
    [int]$RpsQuiz       = 50,
    [int]$MaxVus        = 500,
    [string]$Gui        = "grafana",
    [switch]$KeepUp,
    [switch]$SkipInit,
    [switch]$Build,
    [switch]$Clean,
    [switch]$Debug
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
<#
.SYNOPSIS
  HelloCS 부하테스트를 실행합니다. 먼저 bake-dataset.ps1 로 데이터셋을 생성하세요.

.EXAMPLE
  .\k6\run.ps1 -Dataset default
  .\k6\run.ps1 -Dataset small -Scenario quiz -RpsQuiz 20
  .\k6\run.ps1 -Dataset default -Gui web -KeepUp

.PARAMETER Dataset
  사용할 데이터셋 이름 (ops/perf/datasets/<Name>). 먼저 bake-dataset.ps1 로 생성해야 합니다.

.PARAMETER Scenario
  실행할 시나리오: all / ranking / streak / quiz (기본: all)

.PARAMETER RpsRanking
  랭킹 시나리오 목표 최대 RPS (기본: 200)

.PARAMETER RpsStreak
  스트릭 시나리오 목표 최대 RPS (기본: 100)

.PARAMETER RpsQuiz
  퀴즈 시나리오 목표 최대 RPS (기본: 50)

.PARAMETER MaxVus
  VU 상한 (기본: 500)

.PARAMETER Gui
  "grafana" (기본) | "web" (k6 내장 대시보드) | "none" (CI용)

.PARAMETER KeepUp
  테스트 종료 후 스택을 내리지 않습니다.

.PARAMETER SkipInit
  스택 초기화(down -v → up)를 건너뜁니다. 코드 변경이 없고 재실행만 할 때 사용하세요.

.PARAMETER Build
  Gradle bootJar 로 jar를 빌드한 뒤 이미지를 재빌드해 실행합니다 (bootJar → down -v → up --build). 코드를 변경했을 때 사용하세요.

.PARAMETER Clean
  Prometheus·Grafana 의 수집 데이터를 초기화합니다 (monitoring 볼륨 삭제 후 재시작).

.PARAMETER Debug
  앱 컨테이너 로그를 출력합니다.
#>


$COMPOSE_FILE    = "docker-compose.perf.yml"
$ENV_FILE        = ".env.perf"
$HEALTH_URL      = "http://localhost:8081/actuator/health/readiness"   # gateway management port
$WIREMOCK_URL    = "http://localhost:8089/__admin/health"
$STARTUP_TIMEOUT = 90

$TIMESTAMP    = Get-Date -Format "yyyyMMdd_HHmmss"
$RESULTS_DIR  = "k6\results"
$SUMMARY_FILE = "$RESULTS_DIR\$TIMESTAMP-summary.json"

function Test-UrlReady($url) {
    try {
        $r = Invoke-WebRequest -Uri $url -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

New-Item -ItemType Directory -Force -Path $RESULTS_DIR | Out-Null

# 데이터셋 확인
$datasetDir  = "ops\perf\datasets\$Dataset"
$datasetMeta = "$datasetDir\meta.json"
if (-not (Test-Path $datasetDir)) {
    Write-Host ""
    Write-Host "데이터셋 '$Dataset' 을 찾을 수 없습니다." -ForegroundColor Red
    Write-Host "먼저 실행하세요: .\k6\bake-dataset.ps1 -Name $Dataset" -ForegroundColor Yellow
    exit 1
}

# meta.json 에서 유저/일수 읽기 (k6 토큰 풀 크기 결정)
$seedUserCount = 200
$seedDays      = 30
if (Test-Path $datasetMeta) {
    $meta          = Get-Content $datasetMeta | ConvertFrom-Json
    $seedUserCount = $meta.userCount
    $seedDays      = $meta.days
    Write-Host ""
    Write-Host "=== HelloCS 부하테스트 ===" -ForegroundColor Cyan
    Write-Host "  Dataset: $Dataset (users=$seedUserCount, days=$seedDays)  Scenario: $Scenario" -ForegroundColor DarkGray
} else {
    Write-Host ""
    Write-Host "=== HelloCS 부하테스트 ===" -ForegroundColor Cyan
    Write-Host "  Dataset: $Dataset (meta.json 없음, 기본값 사용)  Scenario: $Scenario" -ForegroundColor DarkGray
}

# ── [1/3] 스택 초기화 ───────────────────────────────────────
Write-Host ""
if ($Clean) {
    Write-Host "[1/3] 모니터링 데이터 초기화 중 (Prometheus·Grafana 볼륨 삭제) ..." -ForegroundColor Yellow
    wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile monitoring down -v 2>&1 | Out-Null
}

if ($SkipInit) {
    Write-Host "[1/3] 스택 초기화 건너뜀 (-SkipInit)" -ForegroundColor DarkGray
    $env:DATASET = $Dataset
    wsl env "DATASET=$Dataset" docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app --profile monitoring up -d
} elseif ($Build) {
    Write-Host "[1/3] 스택 초기화 중 (bootJar → down -v → up --build) ..." -ForegroundColor Yellow
    Write-Host "      Gradle bootJar 빌드 중 (이미지가 이 jar를 복사합니다) ..." -ForegroundColor DarkGray
    & .\gradlew.bat bootJar -x test
    if ($LASTEXITCODE -ne 0) {
        Write-Host "     Gradle bootJar 실패. 이미지 빌드를 중단합니다." -ForegroundColor Red
        exit 1
    }
    $env:DATASET = $Dataset
    wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app down -v 2>&1 | Out-Null
    wsl env "DATASET=$Dataset" docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app --profile monitoring up -d --build
} else {
    Write-Host "[1/3] 스택 초기화 중 (down -v → up, 빌드 생략) ..." -ForegroundColor Yellow
    $env:DATASET = $Dataset
    wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app down -v 2>&1 | Out-Null
    wsl env "DATASET=$Dataset" docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app --profile monitoring up -d
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
    wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE logs wiremock
    if (-not $KeepUp) { wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app down 2>&1 | Out-Null }
    exit 1
}

# cadvisor 준비 대기 (실패 시 경고만 — 부하테스트 자체는 cadvisor 없이도 가능)
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

# ── [2/3] 앱 준비 대기 ──────────────────────────────────────
Write-Host ""
Write-Host "[2/3] 앱 준비 대기 중 (최대 ${STARTUP_TIMEOUT}s) ..." -ForegroundColor Yellow

$elapsed  = 0
$appReady = $false
while ($elapsed -lt $STARTUP_TIMEOUT) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    if (Test-UrlReady $HEALTH_URL) { $appReady = $true; break }
    if ($Debug) {
        wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE logs --since 5s gateway 2>&1 |
            ForEach-Object { Write-Host "  > $_" -ForegroundColor DarkGray }
    }
    Write-Host "     ...${elapsed}s / ${STARTUP_TIMEOUT}s" -ForegroundColor DarkGray
}

if (-not $appReady) {
    Write-Host "     앱 시작 실패. 로그를 확인하세요." -ForegroundColor Red
    wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE logs gateway
    if (-not $KeepUp) { wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app down 2>&1 | Out-Null }
    exit 1
}

try {
    Invoke-WebRequest -Uri "http://localhost:9090/-/reload" -Method POST -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop | Out-Null
    Write-Host "     앱 준비 완료 (Prometheus 리로드 완료)" -ForegroundColor Green
} catch {
    Write-Host "     앱 준비 완료 (Prometheus 리로드 실패 — 무시하고 계속)" -ForegroundColor Yellow
}

# ── [3/3] k6 실행 ───────────────────────────────────────────
Write-Host ""
Write-Host "[3/3] k6 시작..." -ForegroundColor Yellow

$scriptMap = @{
    "all"     = "k6/all.js"
    "ranking" = "k6/scenarios/ranking.js"
    "streak"  = "k6/scenarios/streak.js"
    "quiz"    = "k6/scenarios/quiz.js"
}
$scriptFile = $scriptMap[$Scenario]
if (-not $scriptFile) {
    Write-Host "     Scenario 값이 올바르지 않습니다: $Scenario (all / ranking / streak / quiz)" -ForegroundColor Red
    exit 1
}

$k6Args = @(
    "run",
    "-e", "MAX_RPS_RANKING=$RpsRanking",
    "-e", "MAX_RPS_STREAK=$RpsStreak",
    "-e", "MAX_RPS_QUIZ=$RpsQuiz",
    "-e", "MAX_VUS=$MaxVus",
    "-e", "SEED_USER_COUNT=$seedUserCount",
    "-e", "SEED_DAYS=$seedDays",
    "-e", "SKIP_SEED=true",
    "-e", "DEBUG=$($Debug.IsPresent)",
    "-e", "RESULTS_FILE=$SUMMARY_FILE"
)

$env:K6_PROMETHEUS_RW_SERVER_URL               = "http://localhost:9090/api/v1/write"
$env:K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM = "true"
$k6Args += "--out"; $k6Args += "experimental-prometheus-rw"

if ($Gui -eq "web") {
    $env:K6_WEB_DASHBOARD      = "true"
    $env:K6_WEB_DASHBOARD_OPEN = "true"
    Write-Host "     k6 Dashboard : http://localhost:5665" -ForegroundColor Green
    Write-Host "     Grafana       : http://localhost:3000  (admin / admin)" -ForegroundColor Green
} elseif ($Gui -eq "grafana") {
    Write-Host "     Grafana       : http://localhost:3000  (admin / admin)" -ForegroundColor Green
}

Write-Host "     시나리오      : $Scenario"
Write-Host "     목표 RPS      : 랭킹=$RpsRanking / 스트릭=$RpsStreak / 퀴즈=$RpsQuiz"
Write-Host "     결과 저장     : $SUMMARY_FILE"
Write-Host ""

try {
    k6 @k6Args $scriptFile
} finally {
    if (-not $KeepUp) {
        Write-Host ""
        Write-Host "테스트 완료. 앱/DB 종료 중... (Prometheus/Grafana/cadvisor/node-exporter 유지)" -ForegroundColor Cyan
        wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app down 2>&1 | Out-Null
    } else {
        Write-Host ""
        Write-Host "스택 유지 중 (-KeepUp 플래그). 종료하려면:" -ForegroundColor Cyan
        Write-Host "  wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app down" -ForegroundColor DarkGray
        Write-Host "  (monitoring 스택까지 모두 종료: wsl docker compose --env-file $ENV_FILE -f $COMPOSE_FILE --profile app --profile monitoring down)" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "─────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "  AI 분석을 원하면 Claude Code 에서:" -ForegroundColor Cyan
Write-Host "  /analyze" -ForegroundColor Yellow
Write-Host "─────────────────────────────────────────" -ForegroundColor DarkGray
