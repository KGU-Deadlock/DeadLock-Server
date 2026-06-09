# 관제 개편 타깃 설계 (Phase 2 산출물)

작성 기준일: 2026-06-08  
근거 문서: `INVENTORY.md`  
3대 목적: ① 모듈별 관제 ② 모듈별 흐름·병목 ③ DB별 관제

---

## 라벨 규약 (전 Phase 공통)

| 라벨 | 출처 | 값 예시 |
|---|---|---|
| `application` | 각 서비스 `management.metrics.tags.application` | `quiz-service`, `ranking-service` |
| `service_name` | Tempo span-metrics | `quiz-service` |
| `datname` | postgres_exporter `pg_stat_database_*` | `quiz_db`, `user_db` |
| `queue` | RabbitMQ Prometheus plugin | `ranking.grading.completed` |
| `job` | Prometheus 잡 이름 | `postgres-quiz`, `user-service` |

---

## 1. 스크랩 토폴로지 (→ Phase 3A 담당)

### 현재 상태
- perf prometheus.yml에 `gateway` 1개만 앱 잡으로 등록 + monolith 잔재 `hellocs-app` 잡 존재
- `postgres` 잡이 postgres_exporter 1개(user DB)만 커버
- grading-service, stt-service 컨테이너 자체 미정의 (docker-compose.perf.yml)
- postgres_exporter topic/quiz/interview 없음

### 목표 스크랩 잡 목록

#### 앱 잡 (9개) — management 포트, `/actuator/prometheus`

| job_name | target | relabel `service` |
|---|---|---|
| `gateway` | `gateway:8081` | `gateway` |
| `user-service` | `user-service:8091` | `user-service` |
| `topic-service` | `topic-service:8092` | `topic-service` |
| `quiz-service` | `quiz-service:8093` | `quiz-service` |
| `interview-service` | `interview-service:8094` | `interview-service` |
| `ranking-service` | `ranking-service:8095` | `ranking-service` |
| `streak-service` | `streak-service:8096` | `streak-service` |
| `grading-service` | `grading-service:8097` | `grading-service` |
| `stt-service` | `stt-service:8098` | `stt-service` |

각 앱 잡에 다음 relabel_configs 적용 (job_name만으로 service 라벨 자동 부여):
```yaml
relabel_configs:
  - source_labels: [job]
    target_label: service
```

> `application` 라벨은 micrometer가 각 서비스 metrics에 자동 부여하므로 relabeling 불필요.  
> prometheus 자체 잡(`prometheus:9090`)은 앱 잡과 별도 유지.

#### Postgres 잡 (4개) — exporter별 별도 컨테이너

| job_name | target 컨테이너:포트 | 연결 DB |
|---|---|---|
| `postgres-user` | `postgres_exporter_user:9187` | `postgres-user` / `${USER_POSTGRES_DB}` |
| `postgres-topic` | `postgres_exporter_topic:9188` | `postgres-topic` / `${TOPIC_POSTGRES_DB}` |
| `postgres-quiz` | `postgres_exporter_quiz:9189` | `postgres-quiz` / `${QUIZ_POSTGRES_DB}` |
| `postgres-interview` | `postgres_exporter_interview:9190` | `postgres-interview` / `${INTERVIEW_POSTGRES_DB}` |

기존 `postgres` 잡명 → `postgres-user`로 변경.

#### 인프라 잡 (기존 유지, 잡명 변경 없음)

| job_name | target |
|---|---|
| `rabbitmq` | `rabbitmq:15692` |
| `mongodb` | `mongodb_exporter:9216` |
| `redis` | `redis_exporter:9121` |
| `cadvisor` | `cadvisor:8080` |
| `node-exporter` | `node-exporter:9100` |
| `prometheus` | `prometheus:9090` |

**제거:** `hellocs-app` 잡 (monolith 잔재)

### docker-compose.perf.yml 변경 내역

**추가할 컨테이너:**

1. `grading-service` (app profile)
   - build context: `.`, dockerfile: `grading/Dockerfile`
   - env: `SPRING_PROFILES_ACTIVE=perf`, `SERVER_PORT=8087`, `MANAGEMENT_PORT=8097`
   - env_file: `.env.perf`
   - depends_on: redis (healthy), mongo (healthy), rabbitmq (healthy)
   - deploy limits: `${GRADING_MEMORY_LIMIT}` / `${GRADING_CPU_LIMIT}`
   - healthcheck: `curl http://localhost:8097/actuator/health/readiness`

2. `stt-service` (app profile)
   - build context: `.`, dockerfile: `stt/Dockerfile`
   - env: `SPRING_PROFILES_ACTIVE=perf`, `SERVER_PORT=8088`, `MANAGEMENT_PORT=8098`
   - env_file: `.env.perf`
   - depends_on: rabbitmq (healthy)  ← DB 없음, INVENTORY 확인 결과
   - deploy limits: `${STT_MEMORY_LIMIT}` / `${STT_CPU_LIMIT}`
   - healthcheck: `curl http://localhost:8098/actuator/health/readiness`

3. `postgres_exporter_topic` (app profile)
   - DATA_SOURCE_NAME: `postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@postgres-topic:5432/${TOPIC_POSTGRES_DB}?sslmode=disable`
   - 동일 command/volume 패턴 (queries.yaml, stat_statements, statio_user_tables)
   - ports: `9188:9187` (컨테이너 내부 9187, 호스트 9188)
   - depends_on: postgres-topic (healthy)

4. `postgres_exporter_quiz` (app profile)
   - DATA_SOURCE_NAME: `postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@postgres-quiz:5432/${QUIZ_POSTGRES_DB}?sslmode=disable`
   - ports: `9189:9187`
   - depends_on: postgres-quiz (healthy)

5. `postgres_exporter_interview` (app profile)
   - DATA_SOURCE_NAME: `postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@postgres-interview:5432/${INTERVIEW_POSTGRES_DB}?sslmode=disable`
   - ports: `9190:9187`
   - depends_on: postgres-interview (healthy)

**기존 `postgres_exporter` 컨테이너:** 이름 유지(또는 `postgres_exporter_user`로 변경), 기능 그대로.

**gateway `environment` 추가:**
```yaml
GRADING_SERVICE_HOST: grading-service
GRADING_SERVICE_PORT: 8087
STT_SERVICE_HOST: stt-service
STT_SERVICE_PORT: 8088
```

**gateway `depends_on` 추가:**
```yaml
grading-service:
  condition: service_healthy
stt-service:
  condition: service_healthy
```

### .env.perf.example 추가 변수

```dotenv
# grading-service
GRADING_MEMORY_LIMIT=768m
GRADING_CPU_LIMIT=2.0

# stt-service
STT_MEMORY_LIMIT=512m
STT_CPU_LIMIT=1.0
```

### prometheus.prod.yml 추가 (패리티)

grading-service, stt-service 잡 추가 (management 포트 8097, 8098).  
postgres_exporter 잡은 prod 환경에 exporter 컨테이너 자체가 없으므로 이번 범위 밖 — prod exporter 추가는 별도 결정 필요.

### 변경 파일 귀속 (3A)

- `ops/prometheus/prometheus.yml`
- `ops/prometheus/prometheus.prod.yml`
- `docker-compose.perf.yml`
- `.env.perf.example`

---

## 2. 레코딩 룰 (→ Phase 3B 담당)

### 현재 상태
- `resource.yml`: hikari·tomcat 룰에 `by()` 절 없음 → 전 서비스 합산 스칼라
- `red.yml`: 모든 룰에 `by(..., application)` 포함 → 변경 불필요

### 목표 변경 — resource.yml

#### hikari 풀 사용률

```yaml
# 현재
- record: "instance:hikari_pool:utilization"
  expr: >
    hikaricp_connections_active / hikaricp_connections_max

# 목표
- record: "instance:hikari_pool:utilization"
  expr: >
    sum by (application, pool)
    (hikaricp_connections_active)
    /
    sum by (application, pool)
    (hikaricp_connections_max)
```

#### tomcat 스레드 사용률

```yaml
# 현재
- record: "instance:tomcat_thread:utilization"
  expr: >
    tomcat_threads_busy_threads / tomcat_threads_config_max_threads

# 목표
- record: "instance:tomcat_thread:utilization"
  expr: >
    sum by (application, name)
    (tomcat_threads_busy_threads)
    /
    sum by (application, name)
    (tomcat_threads_config_max_threads)
```

#### jvm heap 사용률 — 변경 없음 (이미 `by(application)`)

### red.yml — 변경 없음

현행 룰 전부 `by(..., application)` 포함. 확인 완료.

### 변경 파일 귀속 (3B)

- `ops/prometheus/rules/resource.yml`

---

## 3. 대시보드 (→ Phase 3B 담당)

공통 규칙:
- Grafana schemaVersion: 39 유지
- datasource uid: `"prometheus"` (Prometheus), `"tempo"` (Tempo)
- 기존 json 파일의 패널 구조·gridPos 패턴 따름

### 3-1. red.json

**변경 항목:**

| 현재 | 목표 |
|---|---|
| up 헬스 패널: `up{job="hellocs-app"}` 하드코딩 | `up{job=~"gateway\|user-service\|topic-service\|quiz-service\|interview-service\|ranking-service\|streak-service\|grading-service\|stt-service"}` 로 교체. stat 패널 → 서비스별 row로 개선 또는 table 패널(각 서비스 상태 표시) |
| `$service` 변수: 이미 동적 | 변경 불필요 — prometheus 스크랩이 고쳐지면 9서비스 자동 표시 |

**Up 패널 목표 PromQL:**

```promql
# 서비스별 UP 상태 table 패널
count by (job) (up{job=~"gateway|user-service|topic-service|quiz-service|interview-service|ranking-service|streak-service|grading-service|stt-service"} == 1)
```

또는 stat 패널로 각 job별 1개씩:
```promql
up{job="gateway"}
up{job="user-service"}
# ... (9개 target 별도 stat 패널 또는 single stat with legend)
```

권장: 기존 stat 패널 1개를 다음으로 교체:
```promql
# expr
up{job=~"gateway|user-service|topic-service|quiz-service|interview-service|ranking-service|streak-service|grading-service|stt-service"}
# legendFormat
{{job}}
```
panel type: `stat`, graphMode: `none`, reduceOptions.calcs: `["lastNotNull"]`, colorMode: `background` (1=green, 0=red threshold)

### 3-2. db.json

**변경 항목:**

#### 템플릿 변수 추가 (현재 `"list": []` → 2개 변수)

```json
{
  "name": "service",
  "label": "서비스",
  "type": "query",
  "datasource": { "type": "prometheus", "uid": "prometheus" },
  "query": "label_values(hikaricp_connections_active, application)",
  "multi": true,
  "includeAll": true,
  "allValue": ".*",
  "refresh": 2,
  "sort": 1
}
```

```json
{
  "name": "db",
  "label": "DB",
  "type": "query",
  "datasource": { "type": "prometheus", "uid": "prometheus" },
  "query": "label_values(pg_stat_database_numbackends, datname)",
  "multi": true,
  "includeAll": true,
  "allValue": ".+",
  "refresh": 2,
  "sort": 1
}
```

#### PostgreSQL 패널 PromQL 변경

| 현재 expr | 목표 expr |
|---|---|
| `pg_stat_database_blks_hit{datname="hellocs"}` | `pg_stat_database_blks_hit{datname=~"$db"}` |
| `pg_stat_database_blks_read{datname="hellocs"}` | `pg_stat_database_blks_read{datname=~"$db"}` |
| `pg_stat_database_deadlocks{datname="hellocs"}` | `pg_stat_database_deadlocks{datname=~"$db"}` |
| `pg_stat_database_numbackends{datname="hellocs"}` | `pg_stat_database_numbackends{datname=~"$db"}` |
| `pg_stat_database_temp_bytes{datname="hellocs"}` | `pg_stat_database_temp_bytes{datname=~"$db"}` |
| `hikaricp_connections_acquire_seconds_bucket{application="hellocs"}` | `hikaricp_connections_acquire_seconds_bucket{application=~"$service"}` |
| `hikaricp_connections_usage_seconds_bucket{application="hellocs"}` | `hikaricp_connections_usage_seconds_bucket{application=~"$service"}` |

슬로우쿼리 패널 (`pg_stat_statements_*`): datname 필터 없음 → `pg_stat_statements_*` 메트릭에 datname 라벨이 있으면 `datname=~"$db"` 추가. 없으면 현행 유지(pg_stat_statements는 DB 단위 exporter이므로 job=~"postgres-.*" 필터로 구분 가능).

### 3-3. resource.json

**현재:** `application=~"$service"` 패턴 이미 적용됨. 변수·패널 구조 정상.

**변경 항목:**

- hikari utilization 패널 (`instance:hikari_pool:utilization`): 레코딩 룰 수정(§2) 후 by(application) 생기므로 legendFormat에 `{{application}}` 추가 확인
- tomcat utilization 패널 (`instance:tomcat_thread:utilization`): 동일
- cadvisor 패널: `container_cpu_usage_seconds_total{container=~"(gateway|user-service|topic-service|quiz-service|interview-service|ranking-service|streak-service|grading-service|stt-service)"}` 로 container 라벨 필터 적용, legendFormat: `{{container}}`

`$service` 변수 query가 `jvm_memory_used_bytes` 기반 → 스크랩 수정 후 9서비스 자동 표시되므로 변경 불필요.

### 3-4. trace.json

**현재:** `$service` 변수가 Tempo span-metrics 기반 동적 조회 → 스크랩 수정(grading/stt 추가) 후 자동 포함.

**변경 항목:**

service-graph 엣지 패널 보강 — 현재 service-graph 패널 존재 여부 확인 후, 없으면 추가:

```promql
# service-graph 엣지 rate (calls/s)
rate(traces_service_graph_request_total{client=~"$service"}[1m])

# service-graph 엣지 error rate
rate(traces_service_graph_request_failed_total{client=~"$service"}[1m])
/ rate(traces_service_graph_request_total{client=~"$service"}[1m])

# service-graph 엣지 p95 레이턴시
histogram_quantile(0.95,
  rate(traces_service_graph_request_duration_seconds_bucket{client=~"$service"}[5m])
)
```

legendFormat 권장: `{{client}} → {{server}}`

### 3-5. messaging.json (신규)

저장 경로: `ops/grafana/provisioning/dashboards/json/messaging.json`

```json
{
  "title": "Messaging — RabbitMQ & 이벤트 체인",
  "uid": "hellocs-messaging",
  "schemaVersion": 39,
  "tags": ["hellocs", "messaging"]
}
```

**패널 구성:**

| Row | 패널 | type | PromQL |
|---|---|---|---|
| RabbitMQ 큐 개요 | 큐별 대기 메시지 수 | timeseries | `rabbitmq_queue_messages_ready{queue=~"ranking.grading.completed\|streak.grading.completed\|streak.interview.completed"}` |
| RabbitMQ 큐 개요 | 큐별 미확인 메시지 수 | timeseries | `rabbitmq_queue_messages_unacked{queue=~"ranking.grading.completed\|streak.grading.completed\|streak.interview.completed"}` |
| RabbitMQ 큐 개요 | 큐별 소비자 수 | stat | `rabbitmq_queue_consumers{queue=~"ranking.grading.completed\|streak.grading.completed\|streak.interview.completed"}` |
| 채점 이벤트 체인 | publish rate | timeseries | `rate(rabbitmq_channel_messages_published_total[1m])` |
| 채점 이벤트 체인 | deliver rate | timeseries | `rate(rabbitmq_queue_messages_delivered_total{queue=~"ranking.grading.completed\|streak.grading.completed"}[1m])` |
| 채점 이벤트 체인 | ack rate | timeseries | `rate(rabbitmq_queue_messages_acked_total{queue=~"ranking.grading.completed\|streak.grading.completed"}[1m])` |
| 채점 이벤트 체인 | 체인 지연 지표 (간접) | timeseries | `rabbitmq_queue_messages_ready{queue=~"ranking.grading.completed\|streak.grading.completed"}` (depth 급증 = 소비 지연) |

> RabbitMQ Prometheus plugin이 export하는 실제 메트릭명은 Prometheus에서 `{job="rabbitmq"}` 조회로 확인.  
> 메트릭명이 다를 경우 (`rabbitmq_detailed_*` 등) Phase 3B가 실제 prometheus에서 확인 후 조정.

**grafana dashboard provider 폴더 확인:**  
`ops/grafana/provisioning/dashboards/` 아래 provider yaml의 path가 `json/` 폴더를 가리키면 `json/messaging.json` 위치로 자동 로드됨. 기존 json 파일들과 동일 폴더에 두면 됨.

### 변경 파일 귀속 (3B)

- `ops/prometheus/rules/resource.yml`
- `ops/grafana/provisioning/dashboards/json/red.json`
- `ops/grafana/provisioning/dashboards/json/db.json`
- `ops/grafana/provisioning/dashboards/json/resource.json`
- `ops/grafana/provisioning/dashboards/json/trace.json`
- `ops/grafana/provisioning/dashboards/json/messaging.json` (신규)

---

## 4. k6 분석 (→ Phase 3C 담당)

### 4-1. collect-metrics.ps1 재설계

**현재 문제:**
- JVM/HikariCP 쿼리 application 필터 없음 → 전 서비스 합산
- PostgreSQL: `datname="hellocs"` 하드코딩 → user DB만
- RabbitMQ 큐 섹션 없음
- http_client_requests 섹션 없음

**목표 출력 구조 (3블록):**

```
=== BLOCK 1: 모듈별 관제 ===

--- [gateway] ---
  HikariCP: N/A (DB 없음)
  JVM: cpu=X% / heap=XMB/XMB / gc_count=X / gc_time=Xs
  Tomcat threads: busy_max=X / max=X

--- [user-service] ---
  HikariCP: pending_max=X / active_max=X / pool_max=X / timeout=X / acquire_max=Xms
  JVM: cpu=X% / heap=XMB/XMB / gc_count=X / gc_time=Xs
  Tomcat threads: busy_max=X / max=X

--- [quiz-service] ---  ... (동일 패턴)
--- [grading-service] ---  ... (동일 패턴, DB없음)
--- [ranking-service] ---  ... (DB없음)
--- [streak-service] ---  ... (DB없음)
--- [interview-service] --- ...
--- [topic-service] ---  ...
--- [stt-service] ---  ...

=== BLOCK 2: 흐름·병목 ===

--- [채점 이벤트 체인] ---
  ranking.grading.completed: depth_max=X / delivered_rate=Xmsg/s / acked_rate=Xmsg/s
  streak.grading.completed:  depth_max=X / delivered_rate=Xmsg/s / acked_rate=Xmsg/s
  streak.interview.completed: depth_max=X / ...

--- [http_client 아웃바운드 레이턴시] ---  (계측 시 추가, 미계측 시 "N/A — ObservationRegistry 미연결")
  quiz→grading: p95=Xms
  quiz→user:    p95=Xms
  ranking→user: p95=Xms

=== BLOCK 3: DB별 관제 ===

--- [PostgreSQL: user DB (${USER_POSTGRES_DB})] ---
  buf_cache_hit=X% / deadlocks=X / backends_max=X / temp_bytes=XMB
  슬로우쿼리 top5: ...

--- [PostgreSQL: topic DB (${TOPIC_POSTGRES_DB})] ---
  ... (동일 패턴)

--- [PostgreSQL: quiz DB (${QUIZ_POSTGRES_DB})] ---
  ...

--- [PostgreSQL: interview DB (${INTERVIEW_POSTGRES_DB})] ---
  ...

--- [Redis] ---
  hit_rate=X% / mem=XMB / clients_max=X / evicted=X / expired=X

--- [MongoDB] ---
  ops_total=X / conn_max=X
```

**구현 지침:**

서비스 루프 — `$services` 배열 정의:
```powershell
$services = @(
  @{ name="gateway";           app="gateway";            hasDB=$false },
  @{ name="user-service";      app="user-service";       hasDB=$true  },
  @{ name="topic-service";     app="topic-service";      hasDB=$true  },
  @{ name="quiz-service";      app="quiz-service";       hasDB=$true  },
  @{ name="interview-service"; app="interview-service";  hasDB=$true  },
  @{ name="ranking-service";   app="ranking-service";    hasDB=$false },
  @{ name="streak-service";    app="streak-service";     hasDB=$false },
  @{ name="grading-service";   app="grading-service";    hasDB=$false },
  @{ name="stt-service";       app="stt-service";        hasDB=$false }
)
```

JVM/HikariCP 쿼리 — application 라벨 필터 적용:
```promql
# HikariCP (per service)
max_over_time(hikaricp_connections_active{application="<app>"}[$lb])
max_over_time(hikaricp_connections_pending{application="<app>"}[$lb])
max_over_time(hikaricp_connections_max{application="<app>"}[$lb])

# JVM CPU
max_over_time(process_cpu_usage{application="<app>"}[$lb])

# JVM Heap
max_over_time(jvm_memory_used_bytes{application="<app>", area="heap"}[$lb])
max_over_time(jvm_memory_max_bytes{application="<app>", area="heap"}[$lb])

# GC
sum(increase(jvm_gc_pause_seconds_count{application="<app>"}[$lb]))
sum(increase(jvm_gc_pause_seconds_sum{application="<app>"}[$lb]))
```

DB 루프 — .env.perf에서 DB명 읽어 배열 구성:
```powershell
$pgDbs = @(
  @{ svc="user";      datname=$env:USER_POSTGRES_DB;      job="postgres-user"      },
  @{ svc="topic";     datname=$env:TOPIC_POSTGRES_DB;     job="postgres-topic"     },
  @{ svc="quiz";      datname=$env:QUIZ_POSTGRES_DB;      job="postgres-quiz"      },
  @{ svc="interview"; datname=$env:INTERVIEW_POSTGRES_DB; job="postgres-interview" }
)
```

.env.perf 로드 (스크립트 상단):
```powershell
if (Test-Path ".env.perf") {
    Get-Content ".env.perf" | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.*)$') { [System.Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim()) }
    }
}
```

RabbitMQ 큐 쿼리:
```promql
# 큐별 대기 메시지 최대값
max_over_time(rabbitmq_queue_messages_ready{queue="ranking.grading.completed"}[$lb])
max_over_time(rabbitmq_queue_messages_ready{queue="streak.grading.completed"}[$lb])
max_over_time(rabbitmq_queue_messages_ready{queue="streak.interview.completed"}[$lb])

# deliver/ack 평균 rate (range query → max value)
rate(rabbitmq_queue_messages_delivered_total{queue="ranking.grading.completed"}[$lb])
rate(rabbitmq_queue_messages_acked_total{queue="ranking.grading.completed"}[$lb])
```

> RabbitMQ 메트릭명은 Prometheus에서 실제 확인 후 조정 (4-3 참조).

http_client_requests (계측 완료 후):
```promql
histogram_quantile(0.95,
  rate(http_client_requests_seconds_bucket{application="quiz-service", uri=~".*/grading.*"}[$lb])
)
```

### 4-2. analyze.md 재작성

`.claude/commands/analyze.md` 분석 단계 구성:

```
1. BLOCK 1 (모듈별) 분석
   - 서비스별 CPU/Heap/GC 이상 서비스 식별
   - HikariCP pending/timeout 발생 서비스 → DB 레이어 병목 판정
   - Tomcat thread saturation 서비스 → 앱 레이어 병목 판정

2. BLOCK 2 (흐름·병목) 분석
   - 채점 이벤트 체인: queue depth 급증 = 소비 지연 → ranking/streak 처리 속도 병목
   - http_client 아웃바운드: p95 > 200ms 서비스 → 서비스간 호출 병목
   - GradingCompletedEvent deliver-ack gap → 소비자 처리 지연 규모 추정

3. BLOCK 3 (DB별) 분석
   - PostgreSQL 4개 DB 개별 분석: buf_cache_hit < 95%, deadlocks > 0, temp_bytes > 0
   - 슬로우쿼리: 앱 쿼리 top3 → 연관 서비스·API 매핑 (k6/CLAUDE.md 슬로우쿼리 힌트 참조)
   - Redis: hit_rate < 90% → ranking 시나리오 캐시 미스, evicted > 0 → maxmemory 검토
   - MongoDB: ops_total 급증 → streak/grading 시나리오 N+1 의심

4. 종합 판정
   - 병목 계층 우선순위: App → DB → 이벤트체인 순 정리
   - 임계값 위반 엔드포인트 + 연관 병목 서비스 매핑
```

### 4-3. analyze.ps1 수정

`k6/analyze.ps1`:
- `docker-compose-local-infra.yaml` → `docker-compose.perf.yml`
- 모델 id: `claude-opus-4-7` → `claude-opus-4-8`
- `/analyze` 스킬이 `collect-metrics.ps1` 결과만 사용하는지 확인 후:
  - `analyze.ps1`가 별도 호출되지 않으면 파일 상단에 주석 `# NOTE: /analyze 스킬은 collect-metrics.ps1 출력을 직접 사용합니다. 이 파일은 독립 실행용입니다.` 추가
  - 완전 미사용 시 사용자 확인 후 제거 결정

### 4-4. k6/CLAUDE.md 정정

| 항목 | 현재 | 목표 |
|---|---|---|
| streak 시나리오 DB 설명 | "PostgreSQL 조회" | "MongoDB 조회 (`user_streaks`, `daily_streak_records`)" |
| 인프라 표 | 단일 Spring Boot + 단일 PostgreSQL | 9개 서비스 + 4 PostgreSQL + Redis + MongoDB + RabbitMQ 구조 |
| grading-service | 미반영 | 채점 이벤트 체인 설명 추가 (quiz→grading→RabbitMQ→ranking/streak) |
| 인프라 표 포트 | 8080/8081(actuator) 단일 행 | 9서비스 전체 포트 맵 (AGENT-PLAN 서비스·포트 맵 기준) |
| threshold 표 | 6 엔드포인트 | 동일 유지 (k6 시나리오가 gateway:8080 경유 — 라우팅 불변) |

### 변경 파일 귀속 (3C)

- `k6/collect-metrics.ps1`
- `.claude/commands/analyze.md`
- `k6/analyze.ps1`
- `k6/CLAUDE.md`

---

## 5. 계측 갭 대응 위치 지정

### 5-1. http_client_requests (아웃바운드 RestClient 계측)

**현재:** RestClient 빌더에 ObservationRegistry 미연결 → `http.client.requests` 메트릭 미수집.

**대응 위치:** `common-web` 모듈의 `WebSupportAutoConfiguration.java`

추가 Bean:
```java
@Bean
@ConditionalOnMissingBean
public RestClient.Builder observedRestClientBuilder(ObservationRegistry observationRegistry) {
    return RestClient.builder()
        .observationRegistry(observationRegistry);
}
```

각 서비스의 RestClient 어댑터(SolvedQuizIdsRestAdapter, UserLevelRestAdapter, etc.)가 `RestClient.Builder`를 주입받아 사용하면 자동 계측됨.

> **Phase 3에서 구현 여부 결정.** 이번 Phase 2는 위치만 지정.  
> 계측 시 메트릭명: `http.client.requests` (라벨: uri, method, status, outcome, client.name)

### 5-2. RabbitMQ Spring AMQP Observation

**현재:** `common-amqp`의 `RabbitMQConfig.java`에 Jackson2JsonMessageConverter만 설정.

**대응 위치:** `common-amqp` 모듈의 `RabbitMQConfig.java` — `SimpleRabbitListenerContainerFactory` Bean에 `setObservationEnabled(true)` 추가 또는 `spring.rabbitmq.listener.simple.observation-enabled=true` 설정.

> 이번 Phase 3 범위에 포함할지는 구현 복잡도 고려 후 3A/3B 담당자 판단.  
> 큐 depth 모니터링(management plugin 15692 기반)으로 이벤트 체인 병목은 간접 추적 가능 — AMQP observation 없이도 Phase 3 완료 가능.

---

## Phase 3 작업 귀속 요약

| Phase | 담당 파일 |
|---|---|
| **3A** 스크랩·익스포터 | `prometheus.yml`, `prometheus.prod.yml`, `docker-compose.perf.yml`, `.env.perf.example` |
| **3B** 룰·대시보드 | `rules/resource.yml`, `red.json`, `db.json`, `resource.json`, `trace.json`, `messaging.json`(신규) |
| **3C** k6·분석 | `collect-metrics.ps1`, `.claude/commands/analyze.md`, `k6/analyze.ps1`, `k6/CLAUDE.md` |
