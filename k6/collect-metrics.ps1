param(
    [string]$TargetHost = $(if ($env:PERF_TARGET_HOST) { $env:PERF_TARGET_HOST } else { "localhost" })
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "SilentlyContinue"

# ── 0. .env.perf 로드 (DB명 등 변수 주입) ────────────────────────────────────
foreach ($envPath in @(".env.perf", "k6\.env.perf", "..\.env.perf")) {
    if (Test-Path $envPath) {
        Get-Content $envPath | ForEach-Object {
            if ($_ -match '^\s*([^#=]+)=(.*)$') {
                [System.Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim())
            }
        }
        break
    }
}

# ── 1. Latest summary ────────────────────────────────────────────────────────
$sf = Get-ChildItem "k6\results\*-summary.json" |
      Sort-Object LastWriteTime -Descending |
      Select-Object -First 1
if (-not $sf) {
    Write-Output "ERROR: k6/results/ 에 summary 파일이 없습니다. run.ps1 을 먼저 실행하세요."
    exit 1
}

$raw  = Get-Content $sf.FullName -Raw | ConvertFrom-Json
$m    = $raw.metrics
$s    = $raw.state
$chks = $raw.root_group.checks

# ── 2. Test time window (KST → Unix) ─────────────────────────────────────────
$tsStr     = ($sf.BaseName -replace '-summary','')          # "20260521_235543"
$startKST  = [DateTime]::ParseExact($tsStr, "yyyyMMdd_HHmmss", $null)
$startUTC  = $startKST.AddHours(-9)
$startUnix = [DateTimeOffset]::new($startUTC, [TimeSpan]::Zero).ToUnixTimeSeconds()
$durSec    = [int]($s.testRunDurationMs / 1000)
$endUnix   = $startUnix + $durSec + 30
$nowUnix   = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$lbSec     = [int]($nowUnix - $startUnix) + 120
$lb        = "${lbSec}s"

# ── 3. Prometheus helpers ─────────────────────────────────────────────────────
$PROM   = "http://${TargetHost}:9090/api/v1"
$promOK = $false
try { Invoke-RestMethod "http://${TargetHost}:9090/-/healthy" -TimeoutSec 2 | Out-Null; $script:promOK = $true } catch {}

function pq([string]$q) {
    if (-not $script:promOK) { return $null }
    try {
        $r = Invoke-RestMethod "$script:PROM/query?query=$([Uri]::EscapeDataString($q))" -TimeoutSec 5
        if ($r.status -eq 'success') { return $r.data.result }
    } catch {}
    return $null
}

function pr([string]$q, [long]$t0, [long]$t1, [int]$step = 15) {
    if (-not $script:promOK -or $t0 -eq 0) { return $null }
    try {
        $uri = "$script:PROM/query_range?query=$([Uri]::EscapeDataString($q))&start=$t0&end=$t1&step=$step"
        $r   = Invoke-RestMethod $uri -TimeoutSec 10
        if ($r.status -eq 'success') { return $r.data.result }
    } catch {}
    return $null
}

function mxv($res) {
    if (-not $res -or $res.Count -eq 0) { return $null }
    $all = @()
    foreach ($r in $res) {
        if ($r.value)  { $all += [double]$r.value[1] }
        if ($r.values) { $all += $r.values | ForEach-Object { [double]$_[1] } }
    }
    if ($all.Count -eq 0) { return $null }
    return ($all | Measure-Object -Maximum).Maximum
}

# ── 4. 관제 토폴로지 정의 ─────────────────────────────────────────────────────
# 모듈별 관제 — 7개 서비스 baseline (application 라벨 기준)
# interview-service / stt-service 는 extra 프로파일 — baseline 미가동
$services = @(
    @{ name="gateway";        app="gateway";        hasDB=$false },
    @{ name="user-service";   app="user-service";   hasDB=$true  },
    @{ name="topic-service";  app="topic-service";  hasDB=$true  },
    @{ name="quiz-service";   app="quiz-service";   hasDB=$true  },
    @{ name="ranking-service"; app="ranking-service"; hasDB=$false },
    @{ name="streak-service"; app="streak-service"; hasDB=$false },
    @{ name="grading-service"; app="grading-service"; hasDB=$false }
)

# DB별 관제 — PostgreSQL 3개 DB (DB당 postgres_exporter)
$pgDbs = @(
    @{ svc="user";  datname=$env:USER_POSTGRES_DB;  job="postgres-user"  },
    @{ svc="topic"; datname=$env:TOPIC_POSTGRES_DB; job="postgres-topic" },
    @{ svc="quiz";  datname=$env:QUIZ_POSTGRES_DB;  job="postgres-quiz"  }
)

# 채점 이벤트 체인 — RabbitMQ 큐 (grading.completed 팬아웃 → ranking + streak)
$queues = @(
    "ranking.grading.completed",
    "streak.grading.completed"
)

# 아웃바운드 호출 소스 서비스 (http_client_requests 계측 시)
$httpClients = @("quiz-service", "ranking-service", "grading-service")

# ── 5. Summary / threshold / check helpers ────────────────────────────────────
function getm($name) {
    $p = $m.PSObject.Properties[$name]
    if ($p) { return $p.Value }
    return $null
}

function ep($key) {
    $mt = getm "http_req_duration{name:$key}"
    if (-not $mt) { return $null }
    $th = if ($mt.thresholds) { $mt.thresholds.PSObject.Properties | Select-Object -First 1 } else { $null }
    return [PSCustomObject]@{
        p50   = $mt.values.'p(50)'
        p95   = $mt.values.'p(95)'
        p99   = $mt.values.'p(99)'
        thKey = if ($th) { $th.Name } else { "" }
        thOk  = if ($th) { $th.Value.ok } else { $null }
    }
}

function getc($name) { $chks | Where-Object { $_.name -eq $name } | Select-Object -First 1 }

function erate($chkName) {
    $c = getc $chkName
    if (-not $c) { return "N/A" }
    $t = [int]$c.passes + [int]$c.fails
    if ($t -eq 0) { return "N/A" }
    "{0:F1}% ({1}/{2})" -f (([int]$c.fails / $t) * 100), [int]$c.fails, $t
}

function fmtN($v, $fmt = "{0:F0}") { if ($null -eq $v) { "N/A" } else { $fmt -f $v } }
function fmtB($b) { if ($null -eq $b) { "?" } elseif ($b) { "PASS" } else { "FAIL" } }
function fmtP($v) { if ($null -eq $v) { "N/A" } else { "{0:F1}%" -f ($v * 100) } }
function fmtMB($b) { if ($null -eq $b) { "N/A" } else { "{0:F0} MB" -f ($b / 1MB) } }
function fmtMs($v) { if ($null -eq $v) { "N/A" } else { "{0:F0}ms" -f ($v * 1000) } }

# ── 6. Build output ───────────────────────────────────────────────────────────
$o = [System.Text.StringBuilder]::new()
function L($line = "") { [void]$o.AppendLine($line) }

$ps = if (-not $promOK) { " (Prometheus 미기동)" } else { "" }

# ── 6-0. K6 요약 + threshold + 모듈 에러율 (k6 결과 기반) ──────────────────────
$failed    = getm "http_req_failed"
$failRate  = if ($failed) { "{0:F2}%" -f ($failed.values.rate * 100) } else { "N/A" }
$failOk    = if ($failed -and $failed.thresholds) {
    fmtB ($failed.thresholds.PSObject.Properties | Select-Object -First 1).Value.ok
} else { "?" }

$httpReqs = getm "http_reqs"
$dropped  = getm "dropped_iterations"
$vusMax   = getm "vus_max"

L "=== K6 SUMMARY ==="
L "파일     : $($sf.Name)"
L "테스트   : $($startKST.ToString('yyyy-MM-dd HH:mm:ss')) KST  |  지속: ${durSec}s  |  최대 VU: $(if ($vusMax) {[int]$vusMax.values.max} else {'N/A'})"
L "총 요청  : $(if ($httpReqs) {[int]$httpReqs.values.count} else {'N/A'})  |  평균 RPS: $(if ($httpReqs) {'{0:F1}' -f $httpReqs.values.rate} else {'N/A'})"
L "실패율   : $failRate  [$failOk]"
L "dropped  : $(if ($dropped) {[int]$dropped.values.count} else {0})건"
L ""

L "=== THRESHOLDS ==="
L "http_req_failed : $failRate [$failOk]"
foreach ($key in @("ranking_summary","ranking_detail","streak_summary","streak_detail","quiz_fetch","quiz_grading")) {
    $e = ep $key
    if ($e) {
        L ("  {0,-20}: p50={1,7:F0}ms  p95={2,8:F2}ms  p99={3,7:F0}ms  /{4}  → {5}" -f $key, $e.p50, $e.p95, $e.p99, $e.thKey, (fmtB $e.thOk))
    } else {
        L "  $key : N/A"
    }
}
L ""

L "=== MODULE ERROR RATES ==="
@(
    @("ranking_summary", "ranking_summary: status 200"),
    @("ranking_detail ", "ranking_detail: status 200"),
    @("streak_summary ", "streak_summary: status 200"),
    @("streak_detail  ", "streak_detail: status 200"),
    @("quiz_fetch     ", "quiz_fetch: status 200"),
    @("quiz_grading   ", "quiz_grading: status 200")
) | ForEach-Object {
    L "  $($_[0]): $(erate $_[1])"
}
L ""

# ══════════════════════════════════════════════════════════════════════════════
#  BLOCK 1: 모듈별 관제 (서비스별 HikariCP / JVM / Tomcat)
# ══════════════════════════════════════════════════════════════════════════════
L "=== BLOCK 1: 모듈별 관제$ps ==="
L ""
foreach ($svc in $services) {
    $app = $svc.app
    L "--- [$($svc.name)] ---"

    # HikariCP (DB 사용 서비스만)
    if ($svc.hasDB) {
        $hikPend    = mxv(pq "max_over_time(hikaricp_connections_pending{application=`"$app`"}[$lb])")
        $hikAct     = mxv(pq "max_over_time(hikaricp_connections_active{application=`"$app`"}[$lb])")
        $hikMax     = mxv(pq "max_over_time(hikaricp_connections_max{application=`"$app`"}[$lb])")
        $hikTimeout = mxv(pq "max_over_time(hikaricp_connections_timeout_total{application=`"$app`"}[$lb])")
        $hikAcquire = mxv(pq "max_over_time(hikaricp_connections_acquire_seconds_max{application=`"$app`"}[$lb])")
        L "  HikariCP    : pending_max=$(fmtN $hikPend) / active_max=$(fmtN $hikAct) / pool_max=$(fmtN $hikMax) / timeout=$(fmtN $hikTimeout) / acquire_max=$(fmtMs $hikAcquire)"
    } else {
        L "  HikariCP    : N/A (DB 없음)"
    }

    # JVM
    $cpu       = mxv(pq "max_over_time(process_cpu_usage{application=`"$app`"}[$lb])")
    $threads   = mxv(pq "max_over_time(jvm_threads_live_threads{application=`"$app`"}[$lb])")
    $heapUsed  = mxv(pq "sum(max_over_time(jvm_memory_used_bytes{application=`"$app`",area=`"heap`"}[$lb]))")
    $heapMax   = mxv(pq "sum(max_over_time(jvm_memory_max_bytes{application=`"$app`",area=`"heap`"}[$lb]))")
    $gcCount   = mxv(pq "sum(increase(jvm_gc_pause_seconds_count{application=`"$app`"}[$lb]))")
    $gcTimeSec = mxv(pq "sum(increase(jvm_gc_pause_seconds_sum{application=`"$app`"}[$lb]))")
    $heapUsedMB = if ($null -ne $heapUsed) { [int]($heapUsed / 1MB) } else { $null }
    $heapMaxMB  = if ($null -ne $heapMax)  { [int]($heapMax / 1MB) }  else { $null }
    L "  JVM         : cpu=$(if ($null -ne $cpu) {'{0:P1}' -f $cpu} else {'N/A'}) / heap=$(if ($null -ne $heapUsedMB){"${heapUsedMB}MB"}else{'N/A'})/$(if ($null -ne $heapMaxMB){"${heapMaxMB}MB"}else{'N/A'}) / threads_max=$(fmtN $threads) / gc_count=$(if ($null -ne $gcCount){[int]$gcCount}else{'N/A'}) / gc_time=$(if ($null -ne $gcTimeSec){'{0:F2}s' -f $gcTimeSec}else{'N/A'})"

    # Tomcat threads (reactive gateway는 N/A 가능)
    $tcBusy = mxv(pq "max_over_time(tomcat_threads_busy_threads{application=`"$app`"}[$lb])")
    $tcMax  = mxv(pq "max_over_time(tomcat_threads_config_max_threads{application=`"$app`"}[$lb])")
    if ($null -ne $tcBusy -or $null -ne $tcMax) {
        L "  Tomcat      : busy_max=$(fmtN $tcBusy) / config_max=$(fmtN $tcMax)"
    } else {
        L "  Tomcat      : N/A (비-Tomcat 또는 미수집)"
    }
    L ""
}

# ══════════════════════════════════════════════════════════════════════════════
#  BLOCK 2: 흐름·병목 (채점 이벤트 체인 / http_client 아웃바운드)
# ══════════════════════════════════════════════════════════════════════════════
L "=== BLOCK 2: 흐름·병목$ps ==="
L ""
L "--- [채점 이벤트 체인 — RabbitMQ 큐] ---"
# 메트릭명은 rabbitmq-prometheus plugin(15692) 기준. 실제 export명이 다르면 (rabbitmq_detailed_*) 조정 필요.
$anyQueueData = $false
foreach ($q in $queues) {
    $depth     = mxv(pq "max_over_time(rabbitmq_queue_messages_ready{queue=`"$q`"}[$lb])")
    $unacked   = mxv(pq "max_over_time(rabbitmq_queue_messages_unacked{queue=`"$q`"}[$lb])")
    $consumers = mxv(pq "max_over_time(rabbitmq_queue_consumers{queue=`"$q`"}[$lb])")
    $delivered = mxv(pq "rate(rabbitmq_queue_messages_delivered_total{queue=`"$q`"}[$lb])")
    $acked     = mxv(pq "rate(rabbitmq_queue_messages_acked_total{queue=`"$q`"}[$lb])")
    if ($null -ne $depth -or $null -ne $consumers) { $anyQueueData = $true }
    L "  $q :"
    L "    depth_max=$(fmtN $depth) / unacked_max=$(fmtN $unacked) / consumers=$(fmtN $consumers) / delivered_rate=$(if ($null -ne $delivered){'{0:F2}msg/s' -f $delivered}else{'N/A'}) / acked_rate=$(if ($null -ne $acked){'{0:F2}msg/s' -f $acked}else{'N/A'})"
}
if (-not $anyQueueData -and $promOK) {
    L "  (큐 지표 없음 — rabbitmq-prometheus plugin 미활성 또는 메트릭명 상이. {job=`"rabbitmq`"} 조회로 실제명 확인)"
}
L ""

L "--- [http_client 아웃바운드 레이턴시 (p95)] ---"
$anyHttpClient = $false
foreach ($app in $httpClients) {
    $p95 = mxv(pq "histogram_quantile(0.95, sum by (le) (rate(http_client_requests_seconds_bucket{application=`"$app`"}[$lb])))")
    if ($null -ne $p95) {
        $anyHttpClient = $true
        L "  ${app} 아웃바운드 : p95=$(fmtMs $p95)"
    }
}
if (-not $anyHttpClient) {
    L "  N/A — http_client_requests 미계측 (common-web RestClient.Builder에 ObservationRegistry 미연결)"
}
L ""

# ══════════════════════════════════════════════════════════════════════════════
#  BLOCK 3: DB별 관제 (PostgreSQL 4 DB / Redis / MongoDB)
# ══════════════════════════════════════════════════════════════════════════════
L "=== BLOCK 3: DB별 관제$ps ==="
L ""

$slowEndUnix = $startUnix + $durSec + 300
foreach ($db in $pgDbs) {
    $dn  = $db.datname
    $job = $db.job
    $label = if ($dn) { "$($db.svc) DB ($dn)" } else { "$($db.svc) DB" }
    L "--- [PostgreSQL: $label] ---"
    if (-not $dn) {
        L "  N/A — .env.perf 미로드 또는 $($db.svc.ToUpper())_POSTGRES_DB 변수 없음"
        L ""
        continue
    }

    $pgHitInc    = mxv(pq "increase(pg_stat_database_blks_hit{datname=`"$dn`"}[$lb])")
    $pgReadInc   = mxv(pq "increase(pg_stat_database_blks_read{datname=`"$dn`"}[$lb])")
    $pgCacheHit  = if ($null -ne $pgHitInc -and $null -ne $pgReadInc -and ($pgHitInc + $pgReadInc) -gt 0) {
        $pgHitInc / ($pgHitInc + $pgReadInc)
    } else { $null }
    $pgDeadlocks = mxv(pq "increase(pg_stat_database_deadlocks{datname=`"$dn`"}[$lb])")
    $pgBackends  = mxv(pq "max_over_time(pg_stat_database_numbackends{datname=`"$dn`"}[$lb])")
    $pgTempBytes = mxv(pq "increase(pg_stat_database_temp_bytes{datname=`"$dn`"}[$lb])")

    # pg_stat_io 는 datname 라벨이 없어 exporter(job) 단위로 구분
    $pgIoHits    = mxv(pq "sum(increase(pg_stat_io_hits{job=`"$job`",object=`"relation`"}[$lb]))")
    $pgIoReads   = mxv(pq "sum(increase(pg_stat_io_reads{job=`"$job`",object=`"relation`"}[$lb]))")
    $pgIoHitRate = if ($null -ne $pgIoHits -and $null -ne $pgIoReads -and ($pgIoHits + $pgIoReads) -gt 0) {
        $pgIoHits / ($pgIoHits + $pgIoReads)
    } else { $null }

    L "  buf_cache_hit=$(fmtP $pgCacheHit) / io_buf_hit=$(fmtP $pgIoHitRate) / deadlocks=$(fmtN $pgDeadlocks) / backends_max=$(fmtN $pgBackends) / temp_bytes=$(fmtMB $pgTempBytes)"

    # 슬로우쿼리 — pg_stat_statements 는 exporter(job) 단위로 해당 DB만 노출
    $slowQ = pr "pg_stat_statements_mean_exec_time{job=`"$job`"}" $startUnix $slowEndUnix 30
    if (-not $slowQ -or $slowQ.Count -eq 0) {
        L "  슬로우쿼리   : 데이터 없음 (DB 종료 후 stale 또는 pg_stat_statements 미수집)"
    } else {
        $setupPfx = @('CREATE ','ALTER ','COPY ','DROP ','INSERT INTO PG_','BEGIN READ','-- ','/*')
        $rows = $slowQ | ForEach-Object {
            $vals  = $_.values | ForEach-Object { [double]$_[1] }
            $maxMs = ($vals | Measure-Object -Maximum).Maximum
            [PSCustomObject]@{ ms = $maxMs; preview = $_.metric.query_preview }
        } | Sort-Object ms -Descending | Select-Object -First 20
        $appRows = $rows | Where-Object { $up = $_.preview.ToUpper(); -not ($setupPfx | Where-Object { $up.StartsWith($_) }) } | Select-Object -First 5
        if ($appRows) {
            L "  슬로우쿼리 top5 (앱 쿼리):"
            foreach ($r in $appRows) { L ("    {0,10:F3}ms | {1}" -f $r.ms, $r.preview) }
        } else {
            L "  슬로우쿼리   : 앱 쿼리 없음 (셋업 쿼리만 존재)"
        }
    }
    L ""
}

# ── Redis ──────────────────────────────────────────────────────────────────────
$redisHitInc  = mxv(pq "increase(redis_keyspace_hits_total[$lb])")
$redisMissInc = mxv(pq "increase(redis_keyspace_misses_total[$lb])")
$redisHitRate = if ($null -ne $redisHitInc -and $null -ne $redisMissInc -and ($redisHitInc + $redisMissInc) -gt 0) {
    $redisHitInc / ($redisHitInc + $redisMissInc)
} else { $null }
$redisMemUsed = mxv(pq "max_over_time(redis_memory_used_bytes[$lb])")
$redisClients = mxv(pq "max_over_time(redis_connected_clients[$lb])")
$redisEvicted = mxv(pq "increase(redis_evicted_keys_total[$lb])")
$redisExpired = mxv(pq "increase(redis_expired_keys_total[$lb])")

L "--- [Redis] ---"
L "  hit_rate=$(fmtP $redisHitRate) / mem_used=$(fmtMB $redisMemUsed) / clients_max=$(fmtN $redisClients) / evicted=$(fmtN $redisEvicted) / expired=$(fmtN $redisExpired)"
L ""

# ── MongoDB ────────────────────────────────────────────────────────────────────
$mongoOpsInc = mxv(pq "sum(increase(mongodb_op_counters_total[$lb]))")
$mongoConns  = mxv(pq "max_over_time(mongodb_connections{state=`"current`"}[$lb])")

L "--- [MongoDB] ---"
L "  ops_total=$(fmtN $mongoOpsInc) / conn_max=$(fmtN $mongoConns)"
L ""

Write-Output $o.ToString()
