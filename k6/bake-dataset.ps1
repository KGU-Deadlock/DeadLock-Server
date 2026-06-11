param(
    [Parameter(Mandatory=$true)]
    [string]$Name,

    [switch]$Build,      # bootJar 재빌드 후 스택 시작 (소스코드 변경 시 사용)
    [switch]$KeepUp,     # 완료 후 bake 스택 유지 (기본: down)
    [switch]$Debug,      # 명령어 출력 표시 (평시에는 억제됨)
    [switch]$DumpOnly    # 실행 중인 bake 스택에서 덤프만 수행 (datasets/<Name>)
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
<#
.SYNOPSIS
  HelloCS 부하테스트용 데이터셋을 세그먼트 분포 모델로 생성합니다.

.DESCRIPTION
  ops/perf/profiles/dataset.env 에서 규모·세그먼트 파라미터를 읽어
  docker-compose.perf.yml + docker-compose.perf.bake.yml 로 MSA 스택을 기동,
  dev-service 를 통해 시딩한 뒤 서비스별 Postgres(×3) + Redis + MongoDB 를 덤프합니다.

  유저는 kakaoId 순서로 power → regular → casual 블록 배치되며,
  계정나이·활동일은 seed 값으로 결정론 생성됩니다 → k6 토큰풀의 블록 슬라이싱과 자동 일치.

  생성되는 파일 (datasets/<Name>/ 폴더):
    postgres-user.sql / postgres-topic.sql / postgres-quiz.sql
    redis/dump.rdb
    mongo/hellocs/
    meta.json

  오류 발생 시: 관련 컨테이너 로그를 자동 수집(콘솔 + <dataset>/_diagnostics/)하고
  스택을 유지하여 수동 조사가 가능하게 합니다.

.EXAMPLE
  .\k6\bake-dataset.ps1 -Name default
  .\k6\bake-dataset.ps1 -Name default -Build
  .\k6\bake-dataset.ps1 -Name default -KeepUp
  .\k6\bake-dataset.ps1 -Name default -Debug

.NOTES
  규모(users, segments)는 ops/perf/profiles/dataset.env 에서 제어합니다.
  소스코드를 변경했다면 -Build 스위치를 사용하세요.
  생성 후: .\k6\run.ps1 -Dataset <Name>
#>

# ── 상수 ─────────────────────────────────────────────────────────────────────

$DatasetProfile = "ops\perf\profiles\dataset.env"
$ComposeBake    = "docker-compose.perf.bake.yml"
$EnvFile        = ".env.perf"
$ProjectName    = "hellocs-bake"

$GwHealthUrl    = "http://localhost:8081/actuator/health/readiness"
$GwAppUrl       = "http://localhost:8080"

$StartupTimeout = 180   # 서비스 빌드·기동 대기 (초)
$SeedTimeout    = 1800  # 통계 시딩 대기 (10k 유저 × 세그먼트 = 최대 30분)

$pgServices = @(
    @{ Service = "postgres-user";  File = "postgres-user.sql"  },
    @{ Service = "postgres-topic"; File = "postgres-topic.sql" },
    @{ Service = "postgres-quiz";  File = "postgres-quiz.sql"  }
)

# ── .env 파서 ────────────────────────────────────────────────────────────────

function Read-EnvFile($path) {
    $map = [ordered]@{}
    if (-not (Test-Path $path)) { return $map }
    Get-Content $path | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$' -and $_ -notmatch '^\s*#') {
            $val = $Matches[2] -replace '\s*#.*$', ''
            $map[$Matches[1]] = $val.Trim()
        }
    }
    return $map
}

# ── dataset.env 읽기 ──────────────────────────────────────────────────────────

$ds = Read-EnvFile $DatasetProfile

$users            = if ($ds["DATASET_USERS"])              { [int]$ds["DATASET_USERS"]              } else { 10000 }
$signupWindowDays = if ($ds["DATASET_SIGNUP_WINDOW_DAYS"]) { [int]$ds["DATASET_SIGNUP_WINDOW_DAYS"] } else { 180   }
$quizPerDay       = if ($ds["DATASET_QUIZ_PER_DAY"])       { [int]$ds["DATASET_QUIZ_PER_DAY"]       } else { 30    }
$quizBank         = if ($ds["DATASET_QUIZ_BANK"])          { [int]$ds["DATASET_QUIZ_BANK"]          } else { 10000 }
$numTopics        = if ($ds["DATASET_TOPICS"])             { [int]$ds["DATASET_TOPICS"]             } else { 6     }
$segPowerShare    = if ($ds["SEG_POWER_SHARE"])            { [double]$ds["SEG_POWER_SHARE"]         } else { 0.2   }
$segRegularShare  = if ($ds["SEG_REGULAR_SHARE"])          { [double]$ds["SEG_REGULAR_SHARE"]       } else { 0.5   }
$segCasualShare   = if ($ds["SEG_CASUAL_SHARE"])           { [double]$ds["SEG_CASUAL_SHARE"]        } else { 0.3   }
$segPowerDpw      = if ($ds["SEG_POWER_DAYS_PER_WEEK"])    { [int]$ds["SEG_POWER_DAYS_PER_WEEK"]    } else { 7     }
$segRegularDpw    = if ($ds["SEG_REGULAR_DAYS_PER_WEEK"])  { [int]$ds["SEG_REGULAR_DAYS_PER_WEEK"]  } else { 4     }
$segCasualDpw     = if ($ds["SEG_CASUAL_DAYS_PER_WEEK"])   { [int]$ds["SEG_CASUAL_DAYS_PER_WEEK"]   } else { 2     }
$tokenPoolSize    = if ($ds["TOKEN_POOL_SIZE"])             { [int]$ds["TOKEN_POOL_SIZE"]            } else { 1000  }
$datasetSeed      = 42L

# ── 공통 헬퍼 ────────────────────────────────────────────────────────────────

function Invoke-Compose {
    param([string[]]$CmdArgs)
    wsl docker compose -p $ProjectName --env-file $EnvFile `
        -f $ComposeBake @CmdArgs
}

# -Debug 시 출력을 표시하고, 평시에는 억제한다.
function Invoke-ComposeQ {
    param([string[]]$CmdArgs)
    if ($script:Debug) {
        Invoke-Compose $CmdArgs
    } else {
        Invoke-Compose $CmdArgs 2>&1 | Out-Null
    }
}

function Invoke-ComposeDown {
    Write-Host "      bake 스택 정리 중..." -ForegroundColor DarkGray
    Invoke-ComposeQ @("--profile", "app", "down", "-v", "--remove-orphans")
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
    if ($LogDir) { Write-Host "      로그 저장됨: $LogDir" -ForegroundColor DarkGray }
    Write-Host "스택을 유지합니다 (수동 조사 가능)." -ForegroundColor Yellow
    Write-Host "정리: wsl docker compose -p $ProjectName --env-file $EnvFile -f $ComposeBake --profile app down -v" -ForegroundColor DarkGray
}

function Invoke-Seed($Label, $Uri, $TimeoutSec = 900) {
    Write-Host "      $Label ..." -NoNewline
    $r = Invoke-WebRequest -Uri $Uri -Method POST -TimeoutSec $TimeoutSec -UseBasicParsing -ErrorAction Stop
    Write-Host " 완료 ($($r.StatusCode))" -ForegroundColor Green
}

# 진행 상황을 5초마다 progress 엔드포인트로 폴링하면서 대기하는 시드 함수 (오래 걸리는 단계 전용)
function Invoke-SeedWithProgress($Label, $Uri, $TimeoutSec = 900) {
    Write-Host "      $Label ..." -ForegroundColor Yellow

    $job = Start-Job -ScriptBlock {
        param($url, $timeout)
        try {
            $r = Invoke-WebRequest -Uri $url -Method POST -TimeoutSec $timeout -UseBasicParsing -ErrorAction Stop
            return @{ StatusCode = [int]$r.StatusCode; Error = $null }
        } catch {
            return @{ StatusCode = 0; Error = $_.ToString() }
        }
    } -ArgumentList $Uri, $TimeoutSec

    $progressUrl = "$GwAppUrl/v1/dev/seed/activity/progress"
    $start       = [DateTime]::UtcNow

    while ($job.State -eq 'Running') {
        Start-Sleep -Seconds 5
        $localElapsed = [int]([DateTime]::UtcNow - $start).TotalSeconds

        try {
            $resp  = Invoke-RestMethod -Uri $progressUrl -Method GET -TimeoutSec 3 -ErrorAction Stop
            $p     = $resp.data
            $phase = $p.phase
            $elSrv = $p.elapsedSeconds

            if ($p.grading -and $p.grading.totalUsers -gt 0) {
                $g   = $p.grading
                $pct = [int]($g.processedUsers * 100 / $g.totalUsers)
                Write-Host ("      [{0}] 유저 {1}/{2} ({3}%) — docs {4} ({5}s)" -f `
                    $phase, $g.processedUsers, $g.totalUsers, $pct, $g.insertedDocs, $elSrv) `
                    -ForegroundColor DarkGray
            } else {
                Write-Host "      [$phase] ${elSrv}s 경과" -ForegroundColor DarkGray
            }
        } catch {
            Write-Host "      ...${localElapsed}s 경과" -ForegroundColor DarkGray
        }
    }

    $result  = Receive-Job -Job $job -Wait
    Remove-Job -Job $job -Force
    $elapsed = [int]([DateTime]::UtcNow - $start).TotalSeconds

    if ($result.Error) {
        Write-Host "      $Label 실패 — ${elapsed}s" -ForegroundColor Red
        throw $result.Error
    }
    Write-Host "      $Label 완료 ($($result.StatusCode)) — ${elapsed}s" -ForegroundColor Green
}

# 4×Postgres → 3 + Redis + Mongo + meta.json 를 $TargetDir 에 덤프한다.
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
        Invoke-ComposeQ @("exec", "-T", "redis", "redis-cli", "BGSAVE")
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
            "postgres-user", "postgres-topic", "postgres-quiz", "redis", "mongo"
        ) $logDir
        throw
    }

    try { $gitSha = (git rev-parse --short HEAD).Trim() } catch { $gitSha = "unknown" }
    if (-not $gitSha) { $gitSha = "unknown" }
    $Meta["createdAt"] = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    $Meta["gitSha"]    = $gitSha
    ($Meta | ConvertTo-Json) |
        Out-File -FilePath (Join-Path ([System.IO.Path]::GetFullPath($TargetDir)) "meta.json") -Encoding utf8
}

# ── 진입점 ───────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "=== HelloCS 데이터셋 Bake: '$Name' ===" -ForegroundColor Cyan
Write-Host "  dataset.env: users=$users  signupWindowDays=$signupWindowDays  quizPerDay=$quizPerDay" -ForegroundColor DarkGray
Write-Host "  세그먼트: power=$segPowerShare/$segPowerDpw dpw  regular=$segRegularShare/$segRegularDpw dpw  casual=$segCasualShare/$segCasualDpw dpw" -ForegroundColor DarkGray
Write-Host ""

$outDir = "ops\perf\datasets\$Name"

# ══════════════════════════════════════════════════════════════════════════════
# DumpOnly: 실행 중인 스택에서 덤프만
# ══════════════════════════════════════════════════════════════════════════════
if ($DumpOnly) {
    Write-Host "[덤프] -DumpOnly: 실행 중인 스택에서 datasets\$Name 으로 덤프" -ForegroundColor Yellow
    $meta = [ordered]@{
        name             = $Name
        users            = $users
        segPowerShare    = $segPowerShare
        segRegularShare  = $segRegularShare
        segCasualShare   = $segCasualShare
        signupWindowDays = $signupWindowDays
        quizPerDay       = $quizPerDay
        quizBank         = $quizBank
        topics           = $numTopics
        seed             = $datasetSeed
    }
    try {
        Invoke-DumpAll $outDir $meta
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
Invoke-ComposeQ @("--profile", "app", "down", "-v", "--remove-orphans")
Write-Host "      완료." -ForegroundColor DarkGray

# ══════════════════════════════════════════════════════════════════════════════
# [2] Bake 스택 시작 (ddl-auto:create, 빈 DB)
# ══════════════════════════════════════════════════════════════════════════════
if ($Build) {
    Write-Host "[2] bootJar 빌드 중..." -ForegroundColor Yellow
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
        -f $ComposeBake --profile app up -d --build
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

    # Docker Compose v2 는 --format json 시 NDJSON 을 출력하므로 Go 템플릿으로 직접 State 를 조회한다.
    $rmqStatus = (Invoke-Compose @("ps", "--format", "{{.State}}", "rabbitmq") 2>$null | Select-Object -First 1)
    if ($rmqStatus) { $rmqStatus = $rmqStatus.Trim() }
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
# [3] 카탈로그 시드 (1회) — 토픽 / 퀴즈뱅크
# ══════════════════════════════════════════════════════════════════════════════
Write-Host ""
Write-Host "[3] 카탈로그 시드 ..." -ForegroundColor Yellow
try {
    Invoke-Seed "topics" "$GwAppUrl/v1/dev/seed/topics"
    Invoke-Seed "quiz-bank (quizBank=$quizBank)" "$GwAppUrl/v1/dev/seed/quiz-bank?perCombo=5"
} catch {
    Write-Host " 실패: $_" -ForegroundColor Red
    Show-Diagnostics "catalog-seed" `
        @("gateway", "dev-service", "topic-service", "quiz-service") `
        "ops\perf\datasets\_bake-errors"
    exit 1
}

# ══════════════════════════════════════════════════════════════════════════════
# [4a] 유저 시드 — PostgreSQL (user-service)
# ══════════════════════════════════════════════════════════════════════════════
Write-Host ""
Write-Host "[4a] 유저 시드 (users=$users) ..." -ForegroundColor Yellow

$usersUrl = "$GwAppUrl/v1/dev/seed/users" +
    "?users=$users" +
    "&numTopics=$numTopics"

try {
    Invoke-Seed "users" $usersUrl
} catch {
    Write-Host " 실패: $_" -ForegroundColor Red
    Show-Diagnostics "users-seed" `
        @("gateway", "dev-service", "user-service") `
        (Join-Path $outDir "_diagnostics")
    exit 1
}

# ══════════════════════════════════════════════════════════════════════════════
# [4b] 채점·스트릭·랭킹 시드 (세그먼트 분포 모델) — MongoDB + Redis
# ══════════════════════════════════════════════════════════════════════════════
Write-Host ""
Write-Host "[4b] 채점·스트릭·랭킹 시드 (세그먼트 분포 모델) — 시간이 걸릴 수 있습니다..." -ForegroundColor Yellow

$activityUrl = "$GwAppUrl/v1/dev/seed/activity" +
    "?users=$users" +
    "&signupWindowDays=$signupWindowDays" +
    "&quizPerDay=$quizPerDay" +
    "&numTopics=$numTopics" +
    "&segPowerShare=$segPowerShare" +
    "&segRegularShare=$segRegularShare" +
    "&segPowerDpw=$segPowerDpw" +
    "&segRegularDpw=$segRegularDpw" +
    "&segCasualDpw=$segCasualDpw" +
    "&tokenPoolSize=$tokenPoolSize" +
    "&seed=$datasetSeed"

try {
    Invoke-SeedWithProgress "seed/activity" $activityUrl $SeedTimeout
} catch {
    Write-Host " 실패: $_" -ForegroundColor Red
    Show-Diagnostics "activity-seed" `
        @("gateway", "dev-service", "grading-service", "streak-service", "ranking-service") `
        (Join-Path $outDir "_diagnostics")
    exit 1
}

# ══════════════════════════════════════════════════════════════════════════════
# [5] 스냅샷 덤프
# ══════════════════════════════════════════════════════════════════════════════
Write-Host ""
Write-Host "[5] 스냅샷 덤프 중..." -ForegroundColor Yellow

$meta = [ordered]@{
    name             = $Name
    users            = $users
    segPowerShare    = $segPowerShare
    segRegularShare  = $segRegularShare
    segCasualShare   = $segCasualShare
    signupWindowDays = $signupWindowDays
    quizPerDay       = $quizPerDay
    quizBank         = $quizBank
    topics           = $numTopics
    seed             = $datasetSeed
}
try {
    Invoke-DumpAll $outDir $meta
} catch {
    Write-Host "덤프 실패. 스택 유지. 재시도: -DumpOnly -Name $Name" -ForegroundColor Red
    exit 1
}
Write-Host "      ✓ datasets\$Name 완료" -ForegroundColor Green

# ══════════════════════════════════════════════════════════════════════════════
# [6] 정리 / 유지
# ══════════════════════════════════════════════════════════════════════════════
Write-Host ""
if ($KeepUp) {
    Write-Host "[6] 스택 유지 중 (-KeepUp)." -ForegroundColor Yellow
    Write-Host "    정리: wsl docker compose -p $ProjectName --env-file $EnvFile -f $ComposeBake --profile app down -v" -ForegroundColor DarkGray
} else {
    Write-Host "[6] bake 스택 정리 중..." -ForegroundColor Yellow
    Invoke-ComposeDown
}

Write-Host ""
Write-Host "=== 데이터셋 '$Name' 생성 완료 ===" -ForegroundColor Cyan
Write-Host "  datasets\$Name  (.\k6\run.ps1 -Dataset $Name)" -ForegroundColor Green
Write-Host ""
