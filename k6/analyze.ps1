# =============================================================
#  HelloCS AI 성능 분석 리포트 생성
#
#  NOTE: /analyze 스킬은 collect-metrics.ps1 출력을 직접 사용합니다.
#        이 파일은 ANTHROPIC_API_KEY 기반 단독 실행용(레거시)입니다.
#
#  단독 실행 예시:
#    $env:ANTHROPIC_API_KEY = "sk-ant-..."
#    .\k6\analyze.ps1
#    .\k6\analyze.ps1 -SummaryFile "k6\results\20260521_120000-summary.json"
# =============================================================

param(
    [string]$SummaryFile = "",
    [string]$OutputDir   = "k6\results"
)

# ── 사전 검증 ─────────────────────────────────────────────
$ANTHROPIC_API_KEY = $env:ANTHROPIC_API_KEY
if (-not $ANTHROPIC_API_KEY) {
    Write-Host "[analyze] ANTHROPIC_API_KEY 가 설정되지 않았습니다." -ForegroundColor Red
    Write-Host "  `$env:ANTHROPIC_API_KEY = 'sk-ant-...'" -ForegroundColor Yellow
    exit 1
}

if (-not $SummaryFile) {
    $latest = Get-ChildItem "$OutputDir\*-summary.json" -ErrorAction SilentlyContinue |
              Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $latest) {
        Write-Host "[analyze] summary 파일 없음. run.ps1 을 먼저 실행하세요." -ForegroundColor Red
        exit 1
    }
    $SummaryFile = $latest.FullName
}

if (-not (Test-Path $SummaryFile)) {
    Write-Host "[analyze] 파일을 찾을 수 없습니다: $SummaryFile" -ForegroundColor Red
    exit 1
}

$ACTUATOR   = "http://localhost:8081/actuator"
$TIMESTAMP  = Get-Date -Format "yyyyMMdd_HHmmss"
$REPORT_FILE = "$OutputDir\$TIMESTAMP-report.md"

Write-Host ""
Write-Host "=== HelloCS AI 성능 분석 ===" -ForegroundColor Cyan
Write-Host "  Summary : $SummaryFile"

# ── 1. k6 summary 읽기 ───────────────────────────────────
$k6Summary = Get-Content $SummaryFile -Raw -Encoding UTF8

# ── 2. PostgreSQL 슬로우 쿼리 ────────────────────────────
Write-Host "  슬로우 쿼리 수집 중..." -ForegroundColor DarkGray

$slowQuerySql = @"
SELECT
  LEFT(query, 120)                        AS query,
  calls,
  ROUND(mean_exec_time::numeric, 2)       AS avg_ms,
  ROUND(total_exec_time::numeric, 0)      AS total_ms,
  ROUND(stddev_exec_time::numeric, 2)     AS stddev_ms
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 15;
"@

try {
    $pgUser = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { "hellocs" }
    $pgDb   = if ($env:USER_POSTGRES_DB) { $env:USER_POSTGRES_DB } else { "user_db" }
    $slowQueries = docker compose -f docker-compose.perf.yml exec -T postgres-user `
        psql -U $pgUser -d $pgDb -t -c $slowQuerySql 2>$null
    if (-not $slowQueries) { $slowQueries = "(결과 없음 — pg_stat_statements 데이터 아직 없음)" }
} catch {
    $slowQueries = "(수집 실패: $_)"
}

# ── 3. Spring Actuator 메트릭 ────────────────────────────
Write-Host "  Actuator 메트릭 수집 중..." -ForegroundColor DarkGray

function Get-Metric($name, $tag = $null) {
    try {
        $uri = "$ACTUATOR/metrics/$name"
        if ($tag) { $uri += "?tag=$tag" }
        $r = Invoke-RestMethod -Uri $uri -TimeoutSec 3 -ErrorAction Stop
        return [math]::Round($r.measurements[0].value, 2)
    } catch { return "N/A" }
}

$heapUsedBytes = Get-Metric "jvm.memory.used" "area:heap"
$heapMaxBytes  = Get-Metric "jvm.memory.max"  "area:heap"
$heapUsedMb    = if ($heapUsedBytes -ne "N/A") { [math]::Round($heapUsedBytes / 1MB, 1) } else { "N/A" }
$heapMaxMb     = if ($heapMaxBytes  -ne "N/A") { [math]::Round($heapMaxBytes  / 1MB, 1) } else { "N/A" }

$actuatorJson = [ordered]@{
    jvm_heap_used_mb    = $heapUsedMb
    jvm_heap_max_mb     = $heapMaxMb
    db_pool_active      = Get-Metric "hikaricp.connections.active"
    db_pool_pending     = Get-Metric "hikaricp.connections.pending"
    db_pool_max         = Get-Metric "hikaricp.connections.max"
    db_pool_timeout     = Get-Metric "hikaricp.connections.timeout"
    process_cpu_usage   = Get-Metric "process.cpu.usage"
    threads_live        = Get-Metric "jvm.threads.live"
} | ConvertTo-Json

# ── 4. Claude API 호출 ───────────────────────────────────
Write-Host "  Claude API 호출 중..." -ForegroundColor DarkGray

$prompt = @"
당신은 Spring Boot 백엔드 성능 분석 전문가입니다.
k6 부하테스트 결과, PostgreSQL 슬로우 쿼리, Spring Actuator 메트릭을 종합 분석하고 한국어로 리포트를 작성하세요.

## k6 부하테스트 요약 (JSON)
$k6Summary

## PostgreSQL 슬로우 쿼리 TOP 15 (mean_exec_time 내림차순)
$slowQueries

## Spring Actuator 스냅샷 (테스트 종료 직후)
$actuatorJson

---

아래 형식으로 분석 리포트를 작성하세요.
숫자는 반드시 위 데이터에서 직접 인용하고, 데이터가 없으면 "데이터 없음"으로 명시하세요.

# HelloCS 성능 분석 리포트
**테스트 일시**: $(Get-Date -Format "yyyy-MM-dd HH:mm")

## 1. 종합 요약
- 전체 테스트 통과 여부 (threshold 위반 여부)
- 가장 심각한 문제 2~3가지 (bullet)

## 2. 모듈별 성능 분석
ranking / streak / quiz 각각:
- p95 응답시간 (실측값)
- 에러율
- dropped_iterations 발생 RPS 추정

## 3. 슬로우 쿼리 분석
- 상위 3개 쿼리 해석 (어느 API에서 발생했는지 추정)
- 인덱스 또는 쿼리 최적화 제안

## 4. 병목 레이어 진단
- DB 병목, App 병목, 이벤트 체인(GradingCompletedEvent) 병목 중 어느 쪽인지 근거와 함께 진단

## 5. 개선 권고안
우선순위 순으로 구체적인 조치 항목 (예: 특정 쿼리에 인덱스 추가, DB pool 크기 조정 등)

## 6. 다음 테스트 권고사항
이번 결과를 바탕으로 다음 테스트에서 조정할 RPS 값 또는 시나리오 제안
"@

$requestBody = @{
    model      = "claude-opus-4-8"
    max_tokens = 4096
    messages   = @(@{ role = "user"; content = $prompt })
} | ConvertTo-Json -Depth 5 -Compress

try {
    $response = Invoke-RestMethod `
        -Uri "https://api.anthropic.com/v1/messages" `
        -Method POST `
        -Headers @{
            "x-api-key"         = $ANTHROPIC_API_KEY
            "anthropic-version" = "2023-06-01"
            "content-type"      = "application/json"
        } `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($requestBody)) `
        -TimeoutSec 120

    $report = $response.content[0].text
} catch {
    Write-Host "[analyze] Claude API 호출 실패: $_" -ForegroundColor Red
    exit 1
}

# ── 5. 리포트 저장 ───────────────────────────────────────
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$report | Out-File -FilePath $REPORT_FILE -Encoding UTF8

Write-Host ""
Write-Host "=== 분석 완료 ===" -ForegroundColor Green
Write-Host "  리포트 : $REPORT_FILE" -ForegroundColor Green
Write-Host ""
Write-Host $report
