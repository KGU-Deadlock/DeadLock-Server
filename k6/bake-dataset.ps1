param(
    [Parameter(Mandatory=$true)]
    [string]$Name,

    # 증분(누적) 마일스톤. 예: -Milestones 100,1000,10000
    #   → datasets/<Name>-100, <Name>-1000, <Name>-10000 을 같은 스택에서 누적 생성.
    #   비워두면 단일 모드($UserCount 1회)로 동작하며 datasets/<Name> 에 저장한다.
    [int[]]$Milestones  = @(),

    [int]$UserCount     = 100,   # 단일 모드 유저 수 (Milestones 미지정 시)
    [int]$Days          = 30,
    [int]$QuizPerCombo  = 5,

    [switch]$Build,      # bootJar 재빌드 후 스택 시작 (소스코드 변경 시 사용)
    [switch]$TearDown,   # 증분 모드에서 끝나고 스택을 정리 (기본: 유지)
    [switch]$DumpOnly    # 실행 중인 bake 스택에서 덤프만 수행 (단일 모드, datasets/<Name>)
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
<#
.SYNOPSIS
  HelloCS 부하테스트용 데이터셋을 MSA 스택으로 생성합니다.

.DESCRIPTION
  docker-compose.perf.yml + docker-compose.perf.bake.yml(오버라이드)로 서비스를 기동,
  dev-service 를 통해 시딩한 뒤 서비스별 Postgres(×4) + Redis + MongoDB 를 분리 덤프합니다.

  [증분 모드] -Milestones 100,1000,10000
    스택을 1회만 기동하고, 카탈로그(토픽/퀴즈뱅크/CS질문)를 1회 시드한 뒤,
    각 마일스톤에서 '직전 마일스톤 이후 새 유저에게만' 채점 이벤트를 발행(fromIndex)하고
    누적 상태를 스냅샷 덤프합니다. ranking(ZINCRBY)·streak 누적 오염 없이 추가-추가 가능.

  생성되는 파일 (단일 모드는 <Name>, 증분 모드는 <Name>-<milestone> 폴더마다):
    postgres-user.sql / postgres-topic.sql / postgres-quiz.sql / postgres-interview.sql
    redis/dump.rdb
    mongo/hellocs/
    meta.json

  오류 발생 시: 관련 컨테이너 로그를 자동 수집(콘솔 + <dataset>/_diagnostics/)하고
  스택을 유지하여 수동 조사가 가능하게 합니다.

.EXAMPLE
  .\k6\bake-dataset.ps1 -Name default
  .\k6\bake-dataset.ps1 -Name small -UserCount 50 -Days 14
  .\k6\bake-dataset.ps1 -Name scale -Milestones 100,1000,10000 -Days 30
  .\k6\bake-dataset.ps1 -Name scale -Milestones 100,1000,10000 -TearDown

.NOTES
  소스코드를 변경했다면 -Build 스위치를 사용하세요 (./gradlew bootJar → compose --build).
  -Build 없이 실행하면 기존 jar를 그대로 사용합니다 (compose --build 는 jar 를 COPY 만 함).
  이후 .\k6\run.ps1 -Dataset <Name>[-<milestone>] 으로 사용합니다.
#>

# ── 상수 ─────────────────────────────────────────────────────────────────────

$ComposeBase  = "docker-compose.perf.yml"
$ComposeBake  = "docker-compose.perf.bake.yml"
$EnvFile      = ".env.perf"
$ProjectName  = "hellocs-bake"

$GwHealthUrl  = "http://localhost:8081/actuator/health/readiness"   # gateway management
$GwAppUrl     = "http://localhost:8080"

$StartupTimeout = 180   # 서비스 빌드·기동 대기 (초)
$DrainTimeout   = 120   # RabbitMQ 큐 드레인 대기 (초)

$pgServices = @(
    @{ Service = "postgres-user";      File = "postgres-user.sql"      },
    @{ Service = "postgres-topic";     File = "postgres-topic.sql"     },
    @{ Service = "postgres-quiz";      File = "postgres-quiz.sql"      },
    @{ Service = "postgres-interview"; File = "postgres-interview.sql" }
)

# ── 공통 헬퍼 ────────────────────────────────────────────────────────────────

function Invoke-Compose {
    param([string[]]$CmdArgs)
    wsl docker compose -p $ProjectName --env-file $EnvFile `
        -f $ComposeBase -f $ComposeBake @CmdArgs
}

function Invoke-ComposeDown {
    Write-Host "      bake 스택 정리 중..." -ForegroundColor DarkGray
    Invoke-Compose @("--profile", "app", "down", "-v", "--remove-orphans") 2>&1 | Out-Null
    Write-Host "      정리 완료." -ForegroundColor DarkGray
}

function Test-UrlReady($url) {
    try {
        $r = Invoke-WebRequest -Uri $url -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

function Get-ContainerId($Service) {
    $id = (Invoke-Compose @("ps", "-q", $Service)) | Select-Object -First 1
    return "$id".Trim()
}

function To-WslPath($WinDir) {
    $full = [System.IO.Path]::GetFullPath($WinDir)
    return '/mnt/' + $full[0].ToString().ToLower() + ($full.Substring(2) -replace '\\', '/')
}

# 실패 단계의 관련 컨테이너 로그를 콘솔 + 파일로 자동 수집한다.
function Show-Diagnostics {
    param(
        [string]$Phase,
        [string[]]$Services,
        [string]$LogDir
    )
    Write-Host ""
    Write-Host "════════ 진단: '$Phase' 단계 실패 ════════" -ForegroundColor Red
    if ($LogDir) { New-Item -ItemType Directory -Force -Path $LogDir | Out-Null }

    Write-Host "--- 컨테이너 상태 (compose ps) ---" -ForegroundColor Yellow
    $ps = Invoke-Compose @("ps", "--format", "table {{.Name}}\t{{.State}}\t{{.Status}}") 2>&1
    $ps | Out-Host
    if ($LogDir) { $ps | Out-File -FilePath (Join-Path $LogDir "_compose-ps.txt") -Encoding utf8 }

    foreach ($svc in $Services) {
        Write-Host "--- $svc (마지막 80줄) ---" -ForegroundColor Yellow
        $log = Invoke-Compose @("logs", "--tail=80", "--no-color", $svc) 2>&1
        $log | Out-Host
        if ($LogDir) { $log | Out-File -FilePath (Join-Path $LogDir "$svc.log") -Encoding utf8 }
    }
    if ($LogDir) {
        Write-Host "      로그 저장됨: $LogDir" -ForegroundColor DarkGray
    }
    Write-Host "스택을 유지합니다 (수동 조사 가능)." -ForegroundColor Yellow
    Write-Host "정리: wsl docker compose -p $ProjectName --env-file $EnvFile -f $ComposeBase -f $ComposeBake --profile app down -v" -ForegroundColor DarkGray
}

function Invoke-Seed($Label, $Uri, $TimeoutSec = 900) {
    Write-Host "      $Label ..." -NoNewline
    $r = Invoke-WebRequest -Uri $Uri -Method POST -TimeoutSec $TimeoutSec -UseBasicParsing -ErrorAction Stop
    Write-Host " 완료 ($($r.StatusCode))" -ForegroundColor Green
}

# RabbitMQ 모든 큐가 빌 때까지 대기. 성공 시 $true, 타임아웃 시 $false.
function Invoke-Drain {
    param([int]$TimeoutSec = $DrainTimeout)
    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        Start-Sleep -Seconds 5
        $elapsed += 5
        $queueLines = Invoke-Compose @("exec", "-T", "rabbitmq",
            "rabbitmqctl", "list_queues", "name", "messages") 2>$null
        $pending = $queueLines |
            Where-Object { $_ -match '^\S+\s+\d+$' } |
            ForEach-Object { ($_ -split '\s+')[1] -as [int] } |
            Measure-Object -Sum | Select-Object -ExpandProperty Sum
        if ($null -eq $pending) { $pending = 0 }
        if ($pending -eq 0) { return $true }
        Write-Host "      대기 중 (미처리 ${pending}개) ... ${elapsed}s / ${TimeoutSec}s" -ForegroundColor DarkGray
    }
    return $false
}

# 4×Postgres + Redis + Mongo + meta.json 을 $TargetDir 에 덤프한다.
# 실패 시 진단 출력 후 예외를 다시 던진다(호출부에서 keep/exit 처리).
function Invoke-DumpAll {
    param(
        [string]$TargetDir,
        [hashtable]$Meta
    )
    New-Item -ItemType Directory -Force -Path "$TargetDir\redis" | Out-Null
    New-Item -ItemType Directory -Force -Path "$TargetDir\mongo" | Out-Null
    $wsl    = To-WslPath $TargetDir
    $logDir = Join-Path $TargetDir "_diagnostics"

    try {
        foreach ($pg in $pgServices) {
            $svc = $pg.Service; $file = $pg.File
            Write-Host "      [postgres] $svc → $file ..." -NoNewline
            Invoke-Compose @("exec", "-T", $svc,
                "bash", "-c",
                'PGPASSWORD=\$POSTGRES_PASSWORD pg_dump -U \$POSTGRES_USER --clean --if-exists --no-owner --no-acl -d \$POSTGRES_DB > /tmp/dataset.sql')
            if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw "pg_dump 실패 ($svc, exit $LASTEXITCODE)" }
            $pgId = Get-ContainerId $svc
            wsl docker cp "${pgId}:/tmp/dataset.sql" "$wsl/$file"
            if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw "docker cp 실패 ($svc)" }
            Write-Host " 완료" -ForegroundColor Green
        }

        Write-Host "      [redis] BGSAVE ..." -NoNewline
        $before = (Invoke-Compose @("exec", "-T", "redis", "redis-cli", "LASTSAVE")).Trim()
        Invoke-Compose @("exec", "-T", "redis", "redis-cli", "BGSAVE") > $null
        $waited = 0
        do {
            Start-Sleep -Seconds 1
            $waited++
            $after = (Invoke-Compose @("exec", "-T", "redis", "redis-cli", "LASTSAVE")).Trim()
        } while ($after -eq $before -and $waited -lt 30)
        $redisId = Get-ContainerId "redis"
        wsl docker cp "${redisId}:/data/dump.rdb" "$wsl/redis/dump.rdb"
        if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw "redis dump.rdb 복사 실패" }
        Write-Host " 완료" -ForegroundColor Green

        Write-Host "      [mongo] mongodump ..." -NoNewline
        Invoke-Compose @("exec", "-T", "mongo",
            "bash", "-c", 'mkdir -p /tmp/mongodump && mongodump --db=hellocs --out=/tmp/mongodump --quiet')
        if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw "mongodump 실패 (exit $LASTEXITCODE)" }
        $mongoId = Get-ContainerId "mongo"
        wsl docker cp "${mongoId}:/tmp/mongodump/." "$wsl/mongo"
        if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { throw "mongo 덤프 복사 실패" }
        Write-Host " 완료" -ForegroundColor Green
    } catch {
        Write-Host " 실패: $_" -ForegroundColor Red
        Show-Diagnostics "dump ($TargetDir)" @(
            "postgres-user", "postgres-topic", "postgres-quiz", "postgres-interview", "redis", "mongo"
        ) $logDir
        throw
    }

    # meta.json
    try { $gitSha = (git rev-parse --short HEAD).Trim() } catch { $gitSha = "unknown" }
    if (-not $gitSha) { $gitSha = "unknown" }
    $Meta["createdAt"] = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    $Meta["gitSha"]    = $gitSha
    ($Meta | ConvertTo-Json) |
        Out-File -FilePath (Join-Path ([System.IO.Path]::GetFullPath($TargetDir)) "meta.json") -Encoding utf8
}

# ── 마일스톤 정규화 ───────────────────────────────────────────────────────────

$incremental = $Milestones.Count -gt 0
if (-not $incremental) { $Milestones = @($UserCount) }
$Milestones = @($Milestones | Sort-Object -Unique)

Write-Host ""
Write-Host "=== HelloCS 데이터셋 Bake: '$Name' ===" -ForegroundColor Cyan
if ($incremental) {
    Write-Host "  모드: 증분  마일스톤=[$($Milestones -join ', ')]  Days=$Days  QuizPerCombo=$QuizPerCombo" -ForegroundColor DarkGray
} else {
    Write-Host "  모드: 단일  UserCount=$UserCount  Days=$Days  QuizPerCombo=$QuizPerCombo" -ForegroundColor DarkGray
}
Write-Host ""

# ══════════════════════════════════════════════════════════════════════════════
# DumpOnly: 기존 스택에서 단일 폴더(<Name>)로 덤프만
# ══════════════════════════════════════════════════════════════════════════════
if ($DumpOnly) {
    Write-Host "[덤프] -DumpOnly: 기존 스택에서 datasets\$Name 으로 덤프" -ForegroundColor Yellow
    $meta = [ordered]@{
        name = $Name; userCount = $UserCount; days = $Days
        quizPerCombo = $QuizPerCombo; incremental = $false
    }
    try {
        Invoke-DumpAll "ops\perf\datasets\$Name" $meta
    } catch {
        Write-Host "덤프 실패. 스택을 유지합니다. 재시도: -DumpOnly" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
    Write-Host "=== 데이터셋 '$Name' 덤프 완료 ===" -ForegroundColor Cyan
    exit 0
}

# ══════════════════════════════════════════════════════════════════════════════
# [1] 기존 bake 스택 정리
# ══════════════════════════════════════════════════════════════════════════════
Write-Host "[1] 기존 bake 스택 정리 중..." -ForegroundColor Yellow
Invoke-Compose @("--profile", "app", "down", "-v", "--remove-orphans") 2>&1 | Out-Null
Write-Host "      완료." -ForegroundColor DarkGray

# ══════════════════════════════════════════════════════════════════════════════
# [2] Bake 스택 시작 (ddl-auto:create, DATASET="")
# ══════════════════════════════════════════════════════════════════════════════
if ($Build) {
    Write-Host "[2] bootJar 빌드 중 (이미지가 이 jar를 복사합니다) ..." -ForegroundColor Yellow
    & .\gradlew.bat bootJar -x test
    if ($LASTEXITCODE -ne 0) {
        Write-Host "      Gradle bootJar 실패. 스택 시작을 중단합니다." -ForegroundColor Red
        exit 1
    }
    Write-Host "      bootJar 완료." -ForegroundColor Green
}
Write-Host "[2] Bake 스택 시작 중 (ddl-auto:create, 빈 DB) ..." -ForegroundColor Yellow
try {
    wsl env "DATASET=" docker compose -p $ProjectName --env-file $EnvFile `
        -f $ComposeBase -f $ComposeBake --profile app up -d --build
    if ($LASTEXITCODE -ne 0) { throw "docker compose up 실패 (exit code $LASTEXITCODE)" }
} catch {
    Show-Diagnostics "stack-up" @("rabbitmq", "gateway") "ops\perf\datasets\_bake-errors"
    exit 1
}

# 게이트웨이 readiness 대기
$elapsed  = 0
$appReady = $false
Write-Host "      게이트웨이 준비 대기 중 (최대 ${StartupTimeout}s) ..."
while ($elapsed -lt $StartupTimeout) {
    Start-Sleep -Seconds 5
    $elapsed += 5

    $rmqStatus = (Invoke-Compose @("ps", "--format", "json", "rabbitmq") 2>$null |
        ConvertFrom-Json -ErrorAction SilentlyContinue | Select-Object -ExpandProperty State) 2>$null
    if ($rmqStatus -and $rmqStatus -ne "running") {
        Show-Diagnostics "stack-up (rabbitmq 비정상: $rmqStatus)" @("rabbitmq", "gateway") "ops\perf\datasets\_bake-errors"
        exit 1
    }

    if (Test-UrlReady $GwHealthUrl) { $appReady = $true; break }
    Write-Host "      ...${elapsed}s / ${StartupTimeout}s" -ForegroundColor DarkGray
}
if (-not $appReady) {
    Show-Diagnostics "stack-up (readiness 타임아웃)" `
        @("gateway", "dev-service", "user-service", "quiz-service", "rabbitmq") "ops\perf\datasets\_bake-errors"
    exit 1
}
Write-Host "      게이트웨이 준비 완료 (${elapsed}s)" -ForegroundColor Green

# ══════════════════════════════════════════════════════════════════════════════
# [3] 카탈로그 시드 (1회) — 토픽 / 퀴즈뱅크 / CS질문
# ══════════════════════════════════════════════════════════════════════════════
Write-Host ""
Write-Host "[3] 카탈로그 시드 (1회) ..." -ForegroundColor Yellow
try {
    Invoke-Seed "topics" "$GwAppUrl/v1/dev/seed/topics"
    Invoke-Seed "quiz-bank (perCombo=$QuizPerCombo)" "$GwAppUrl/v1/dev/seed/quiz-bank?perCombo=$QuizPerCombo"
} catch {
    Write-Host " 실패: $_" -ForegroundColor Red
    Show-Diagnostics "catalog-seed" `
        @("gateway", "dev-service", "topic-service", "quiz-service", "interview-service") `
        "ops\perf\datasets\_bake-errors"
    exit 1
}

# ══════════════════════════════════════════════════════════════════════════════
# [4] 마일스톤 증분 시드 + 단계별 덤프
# ══════════════════════════════════════════════════════════════════════════════
$prev = 0
foreach ($m in $Milestones) {
    $dsName = if ($incremental) { "$Name-$m" } else { $Name }
    $outDir = "ops\perf\datasets\$dsName"
    Write-Host ""
    Write-Host "[4] 마일스톤 $m  (델타 유저 $prev → $m, days=$Days) → datasets\$dsName" -ForegroundColor Yellow

    # 4a. 통계 시드 (새 유저 [prev, m) 에게만 채점 이벤트 발행)
    try {
        Invoke-Seed "stats (userCount=$m, fromIndex=$prev) — 시간이 걸릴 수 있습니다" `
            "$GwAppUrl/v1/dev/seed/stats?userCount=$m&days=$Days&fromIndex=$prev"
    } catch {
        Write-Host " 실패: $_" -ForegroundColor Red
        Show-Diagnostics "stats-seed (m=$m)" `
            @("gateway", "dev-service", "user-service", "rabbitmq", "ranking-service", "streak-service") `
            (Join-Path $outDir "_diagnostics")
        exit 1
    }

    # 4b. RabbitMQ 드레인 (ranking/streak 반영 완료 대기)
    Write-Host "      RabbitMQ 큐 드레인 대기 중 (최대 ${DrainTimeout}s) ..." -ForegroundColor DarkGray
    if (Invoke-Drain) {
        Write-Host "      큐 비워짐 — ranking·streak 반영 완료" -ForegroundColor Green
    } else {
        Write-Host "      경고: 드레인 타임아웃. 일부 이벤트가 미처리일 수 있어 진단을 남깁니다." -ForegroundColor Yellow
        Show-Diagnostics "drain-timeout (m=$m)" `
            @("rabbitmq", "ranking-service", "streak-service") (Join-Path $outDir "_diagnostics")
        Write-Host "      계속 진행합니다." -ForegroundColor Yellow
    }

    # 4c. 스냅샷 덤프
    Write-Host "      스냅샷 덤프 중..." -ForegroundColor DarkGray
    $meta = [ordered]@{
        name = $dsName; baseName = $Name
        userCount = $m; deltaFrom = $prev; days = $Days
        quizPerCombo = $QuizPerCombo; incremental = $incremental
    }
    try {
        Invoke-DumpAll $outDir $meta
    } catch {
        Write-Host "마일스톤 $m 덤프 실패. 스택 유지. 재시도: -DumpOnly -Name $dsName" -ForegroundColor Red
        exit 1
    }
    Write-Host "      ✓ datasets\$dsName 완료" -ForegroundColor Green
    $prev = $m
}

# ══════════════════════════════════════════════════════════════════════════════
# [5] 정리 / 유지
# ══════════════════════════════════════════════════════════════════════════════
Write-Host ""
if ($TearDown) {
    Write-Host "[5] bake 스택 정리 중 (-TearDown)..." -ForegroundColor Yellow
    Invoke-ComposeDown
} else {
    Write-Host "[5] 스택 유지 중 (추가 마일스톤/조사 가능)." -ForegroundColor Yellow
    Write-Host "    정리: wsl docker compose -p $ProjectName --env-file $EnvFile -f $ComposeBase -f $ComposeBake --profile app down -v" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "=== 데이터셋 '$Name' 생성 완료 ===" -ForegroundColor Cyan
foreach ($m in $Milestones) {
    $dsName = if ($incremental) { "$Name-$m" } else { $Name }
    Write-Host "  datasets\$dsName  (.\k6\run.ps1 -Dataset $dsName)" -ForegroundColor Green
}
Write-Host ""
