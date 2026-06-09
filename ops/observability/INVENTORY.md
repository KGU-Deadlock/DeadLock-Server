# 관제 현황 인벤토리 (Phase 1 산출물)

조사 기준일: 2026-06-08  
범위: 9개 서비스 전체 + ops/prometheus + docker-compose.perf.yml + grafana json 4개 + k6

---

## 1. 서비스별 관제 설정 표

| 서비스 | application 라벨 | Management 포트 | Actuator prometheus | Tracing endpoint | Sampling | Datasource / Redis / Mongo / Rabbit |
|---|---|---|---|---|---|---|
| gateway | `gateway` | 8081 | O | `http://${TEMPO_HOST}:4318/v1/traces` | 1.0 | 없음 |
| user-service | `user-service` | 8091 | O | 동일 | 1.0 | PostgreSQL(${USER_POSTGRES_DB}) + Rabbit |
| topic-service | `topic-service` | 8092 | O | 동일 | 1.0 | PostgreSQL(${TOPIC_POSTGRES_DB}) |
| quiz-service | `quiz-service` | 8093 | O | 동일 | 1.0 | PostgreSQL(${QUIZ_POSTGRES_DB}) + Redis + Mongo + Rabbit |
| interview-service | `interview-service` | 8094 | O | 동일 | 1.0 | PostgreSQL(${INTERVIEW_POSTGRES_DB}) + Rabbit |
| ranking-service | `ranking-service` | 8095 | O | 동일 | 1.0 | Redis + Rabbit |
| streak-service | `streak-service` | 8096 | O | 동일 | 1.0 | Mongo + Rabbit |
| grading-service | `grading-service` | (8097 미정의) | O | 동일 | 1.0 | Redis + Mongo + Rabbit |
| stt-service | `stt-service` | (8098 미정의) | O | 동일 | 1.0 | 없음 (WebSocket 전용) |

**파일 근거:**
- 라벨/포트/tracing: 각 서비스 `application-perf.yaml` (gateway:23, user:30, grading:23, stt:23)
- actuator exposure: 모든 서비스 `application-perf.yaml` L11 `include: health,info,prometheus`
- grading 실제 의존: `grading/application-perf.yaml` — datasource 없음(Redis+Mongo는 `application.yaml`에서 확인 필요)
- stt DB 없음: `stt/application-perf.yaml` — datasource/redis/mongo 설정 미존재
- grading/stt management 포트: `docker-compose.perf.yml`에 컨테이너 미정의 → 포트 미할당

---

## 2. 현재 스크랩 토폴로지

### prometheus.yml (perf 환경) — `ops/prometheus/prometheus.yml`

| job_name | targets | 수집 여부 |
|---|---|---|
| prometheus | prometheus:9090 | O |
| gateway | gateway:8081 | O |
| hellocs-app | app:8081 | **잔재 (monolith 시대 잡)** |
| rabbitmq | rabbitmq:15692 | O (관리 UI 메트릭만) |
| postgres | postgres_exporter:9187 | O (user DB만) |
| mongodb | mongodb_exporter:9216 | O |
| redis | redis_exporter:9121 | O |
| cadvisor | cadvisor:8080 | O |
| node-exporter | node-exporter:9100 | O |

**누락 (perf):**
- user-service (8091), topic-service (8092), quiz-service (8093), interview-service (8094), ranking-service (8095), streak-service (8096) — 잡 미정의
- grading-service (8097), stt-service (8098) — 컨테이너 자체 미정의
- postgres_exporter for topic/quiz/interview DB — 잡·컨테이너 미정의

### prometheus.prod.yml (프로덕션 환경) — `ops/prometheus/prometheus.prod.yml`

| job_name | targets | 수집 여부 |
|---|---|---|
| gateway | gateway:8081 | O |
| user-service | user-service:8091 | O |
| topic-service | topic-service:8092 | O |
| quiz-service | quiz-service:8093 | O |
| interview-service | interview-service:8094 | O |
| ranking-service | ranking-service:8095 | O |
| streak-service | streak-service:8096 | O |
| rabbitmq | rabbitmq:15692 | O |
| cadvisor | cadvisor:8080 | O |
| node-exporter | node-exporter:9100 | O |

**누락 (prod):**
- grading-service, stt-service 잡 미정의
- postgres_exporter 잡 전혀 없음 (perf와 달리 prod는 exporter 자체 없음)
- mongodb_exporter, redis_exporter 잡 없음

### docker-compose.perf.yml — 컨테이너 현황

| 컨테이너 | profiles | 포함 여부 |
|---|---|---|
| gateway, user, topic, quiz, interview, ranking, streak | app | O |
| postgres-user/topic/quiz/interview, redis, mongo, rabbitmq | app | O |
| postgres_exporter | app | O (user DB만) |
| mongodb_exporter, redis_exporter, cadvisor, node-exporter, wiremock | app | O |
| prometheus, grafana, tempo | monitoring | O |
| **grading-service** | — | **미정의** |
| **stt-service** | — | **미정의** |
| **postgres_exporter (topic/quiz/interview)** | — | **미정의** |

`postgres_exporter` DATA_SOURCE_NAME: `postgresql://...@postgres-user:5432/${USER_POSTGRES_DB}` (L481)

---

## 3. 대시보드·룰·스크립트의 Staleness 목록

### Prometheus 룰

| 파일 | 라인 | 증상 |
|---|---|---|
| `resource.yml` | L6-10 | `instance:hikari_pool:utilization` — by() 절 없음 → 전 서비스 합산, per-application 불가 |
| `resource.yml` | L12-17 | `instance:tomcat_thread:utilization` — by() 절 없음 → 동일 문제 |
| `resource.yml` | L20-24 | `instance:jvm_heap:utilization` — `by(application)` 있음 → 정상 |
| `red.yml` | L6-38 | 모든 룰이 `by(..., application)` 포함 → 정상 |

### Grafana 대시보드

| 파일 | 라인 | 증상 |
|---|---|---|
| `red.json` | L124 | `up{job="hellocs-app"}` 하드코딩 → hellocs-app 잡은 monolith 잔재, 9개 서비스 헬스 미표시 |
| `red.json` | L18 | `$service` 변수: `label_values(http_server_requests_seconds_count, application)` → 동적(양호), 단 perf prometheus에 user~streak 잡이 없어 현재 거의 빈 목록 |
| `db.json` | L11 | `"templating": { "list": [] }` → 템플릿 변수 전혀 없음. $service/$db 변수 미정의 |
| `db.json` | L79,84,89,105,110,115 | `application="hellocs"` 하드코딩 (hikaricp_connections_acquire/usage_seconds_bucket) → 어떤 서비스도 매핑 안 됨 |
| `db.json` | L139,155,160,176,192 | `pg_stat_database_*` 쿼리에 datname 필터 없음 → 4개 DB 전체 합산, DB별 분리 불가 |
| `resource.json` | L14-337 | `$service` 변수·모든 패널이 `application=~"$service"` 사용 → 정상. 단 hikari/tomcat utilization 룰에 application 라벨 없어 L307/L219 패널에서 데이터 누락 |
| `trace.json` | L18 | `$service`: `label_values(traces_spanmetrics_duration_milliseconds_count, service_name)` → Tempo span-metrics 기반, 동적(양호). 단 grading/stt span 미수집 중 |

### k6 수집 스크립트

| 파일 | 라인 | 증상 |
|---|---|---|
| `collect-metrics.ps1` | L65-73 | hikaricp, JVM, GC 쿼리 — application 필터 없음, 전 서비스 합산 |
| `collect-metrics.ps1` | L88-95 | `datname="hellocs"` 하드코딩 → user DB 외 topic/quiz/interview 미수집 |
| `collect-metrics.ps1` | 전체 | RabbitMQ 큐 지표 섹션 없음 |
| `collect-metrics.ps1` | 전체 | http_client_requests 아웃바운드 섹션 없음 |
| `k6/CLAUDE.md` | streak 시나리오 설명 | "PostgreSQL 조회"로 오기재 → 실제 streak-service는 MongoDB 사용 |
| `k6/CLAUDE.md` | 인프라 표 | 단일 Spring Boot app + 단일 PostgreSQL 기준 (monolith 시대). 9개 서비스·4 Postgres·grading/stt 미반영 |

---

## 4. 계측 갭

### A. HTTP 아웃바운드 (RestClient) 계측
- **상태:** 미확인. Spring Boot 3.2+ auto-config는 `RestClient`가 ObservationRegistry-enabled 빌더로 생성된 경우 `http.client.requests` 자동 계측.
- **현황:** `MetricsConfig.java`(common-web)는 서버 사이드(URI 라벨 제한)만 설정. RestClient 빌더에 observation 주입 코드 미검출.
- **아웃바운드 호출 지점 (코드 확인):**
  - `SolvedQuizIdsRestAdapter` — quiz→grading 호출
  - `UserLevelRestAdapter` — quiz→user 호출
  - `UserInterestRestAdapter`, `UserProfileRestAdapter` — ranking→user 호출
  - `TopicNameRestAdapter` — grading→topic 호출
  - `AiGradingAdapter`, `AiFeedbackAdapter` — grading/interview→WireMock 호출
- **결론:** 아웃바운드 레이턴시 메트릭이 현재 수집되지 않을 가능성 높음. `common-web`의 RestClient 빌더에 `observationRegistry` 주입 필요 여부 확인 필요.

### B. RabbitMQ 메시지 계측
- **상태:** rabbitmq_exporter(15692)가 관리 UI 메트릭(연결·채널·큐 수)만 수집.
- **큐별 depth/publish/deliver/ack/nack 미수집.**
- **Spring AMQP `RabbitListener` Micrometer 계측:** common-amqp에서 Jackson2JsonMessageConverter만 설정, 리스너 observation 미설정.
- **영향:** GradingCompletedEvent 체인(grading→ranking, grading→streak) 병목 추적 불가.

### C. PostgreSQL DB별 계측
- **상태:** postgres_exporter 1개가 postgres-user DB만 커버.
- topic/quiz/interview DB의 pg_stat_database, pg_stat_statements, pg_stat_io 미수집.
- **영향:** quiz-service(QUIZ_POSTGRES_DB), interview-service(INTERVIEW_POSTGRES_DB)의 슬로우쿼리·캐시히트·데드락 분석 불가.

### D. grading/stt 서비스 계측
- docker-compose.perf.yml에 컨테이너 미정의 → perf 환경에서 부하테스트 시 grading/stt 메트릭 전혀 수집 안 됨.
- 채점 이벤트 체인의 핵심 병목인 grading-service가 관제 사각지대.

---

## 5. RabbitMQ 큐·이벤트 체인 현황

### Exchange
- 이름: `hellocs.events` (TopicExchange, durable)
- 정의: `RabbitMQConfig.java` (common-amqp)

### 큐 목록

| 큐명 | 라우팅 키 | 소비자 | 정의 위치 |
|---|---|---|---|
| `ranking.grading.completed` | `grading.completed` | `RankingEventListener` (@RabbitListener) | `RankingRabbitMQConfig.java` L11,14 |
| `streak.grading.completed` | `grading.completed` | streak listener | `StreakRabbitMQConfig.java` L11,15 |
| `streak.interview.completed` | `interview.completed` | streak listener | `StreakRabbitMQConfig.java` L12,20 |

### 이벤트 발행
- `GradingEventPublishAdapter` (grading-service) — `RabbitTemplate.convertAndSend("hellocs.events", "grading.completed", event)`
- 라우팅 키 `grading.completed` → ranking + streak 큐에 **동시 팬아웃**

### 큐 설정 현황
- `QueueBuilder.durable().build()` 최소 사양만 적용
- TTL, max-length, dead-letter 정책 미정의

---

## 6. 미해결/확인필요 질문

1. **RestClient ObservationRegistry 자동 적용 여부**: Spring Boot 4.0에서 RestClient 빌더 auto-config로 observation 자동 등록되는지 확인 필요. 안 된다면 `common-web`의 RestClient 빌더 Bean에 명시 주입 필요.

2. **grading-service 실제 application.yaml DB 설정**: `grading/application-perf.yaml`에 Redis/Mongo 설정 없음 → `application.yaml`(default) 또는 docker-compose env에서 주입되는지 확인 필요.

3. **stt-service WebSocket 계측 범위**: WebSocket 핸들러 레벨 메트릭(프레임 수/세션 수명) 수집 여부. Spring의 WebSocket metrics auto-instrumentation 범위 확인 필요.

4. **GradingCompletedEvent 발행-소비 지연 측정**: RabbitMQ 관리 UI 메트릭 기반으로 큐 depth 추적은 가능하나, 발행~소비 E2E 레이턴시 측정을 위해 메시지 타임스탬프 또는 Spring AMQP observation 활성화 필요.

5. **prometheus.prod.yml postgres_exporter 부재**: prod 환경에는 postgres_exporter 잡이 전혀 없어 프로덕션 DB 성능 완전 사각지대. perf와 패리티 맞출 때 prod 추가 여부 결정 필요.
