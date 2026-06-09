# MSA 관제 개편 — 에이전트 실행 플랜

MSA 전환 후 관제(k6/ops)를 **① 모듈별 관제 ② 모듈별 흐름·병목 ③ DB별 관제** 목적에 맞게 개편하기 위한 에이전트 작업 지시서.
각 페이즈는 **새 채팅 세션(초기화된 컨텍스트)** 의 에이전트가 받아도 자립 실행되도록 작성됨.

---

## 0. 각 에이전트를 어떻게 호출하나 (요약)

세션이 리셋되므로 **페이즈 간 인수인계는 파일로** 한다.

```
Phase 1 (조사) → ops/observability/INVENTORY.md      작성
Phase 2 (설계) → ops/observability/TARGET-DESIGN.md  작성  (INVENTORY 읽음)
Phase 3 (구현) → 3A / 3B / 3C 병렬                    (TARGET-DESIGN 읽음)
```

의존: **1 → 2 → (3A ∥ 3B ∥ 3C)**. 3A/3B/3C는 만지는 파일이 안 겹쳐 동시 실행 가능.

| 페이즈 | 새 세션에서 이렇게 말하면 됨 | 한 줄 임무 |
|---|---|---|
| **Phase 1** | "이 파일의 `공유 컨텍스트` + `Phase 1` 블록대로 관제 인벤토리 조사해줘. read-only." | 현재 무엇이 어떻게 수집·소비되는지 사실관계 확정 → `INVENTORY.md` |
| **Phase 2** | "`INVENTORY.md` 읽고, 이 파일 `공유 컨텍스트` + `Phase 2` 블록대로 타깃 설계 짜줘. 코드 수정 금지." | "어떻게 나눌지" 목표 사양 확정 → `TARGET-DESIGN.md` |
| **Phase 3A** | "`TARGET-DESIGN.md` 읽고 `공유 컨텍스트` + `Phase 3A` 블록대로 스크랩·익스포터만 수정해줘." | prometheus.yml / compose / .env — 스크랩 토폴로지 |
| **Phase 3B** | "`TARGET-DESIGN.md` 읽고 `공유 컨텍스트` + `Phase 3B` 블록대로 룰·대시보드만 수정해줘." | rules/*.yml / grafana json — 룰·대시보드 |
| **Phase 3C** | "`TARGET-DESIGN.md` 읽고 `공유 컨텍스트` + `Phase 3C` 블록대로 k6·분석만 수정해줘." | collect-metrics.ps1 / analyze.md / k6 docs |

> 호출 시 **반드시** 아래 `공유 컨텍스트`를 함께 주거나, "이 파일(`ops/observability/AGENT-PLAN.md`)의 공유 컨텍스트와 Phase N 블록을 읽고 실행해" 라고 지시할 것.

마지막 검증(`/perf` → `/analyze`)은 **사람이 직접 트리거**한다.

---

## 공유 컨텍스트 (모든 에이전트 프롬프트에 공통 전달)

```text
[프로젝트] HelloCS — Spring Boot 4 / Java 25 MSA. 모놀리식→MSA 전환 직후.
저장소 루트: C:\Users\guna\Desktop\devs\DeadLock-Server (Windows, PowerShell/pwsh 기본).

[서비스·포트·DB·인프라 맵] (app포트 / management포트)
- gateway      8080 / 8081  : DB없음, 전 서비스 라우팅
- user-service 8081 / 8091  : postgres-user,        rabbit
- topic-service 8082/ 8092  : postgres-topic
- quiz-service 8083 / 8093  : postgres-quiz, redis, mongo, rabbit (→user/topic/grading 호출)
- interview-service 8084/8094: postgres-interview,  rabbit
- ranking-service 8085/8095 : DB없음(redis),       rabbit (→user 호출)
- streak-service 8086 / 8096: DB없음(mongo),        rabbit
- grading-service (포트 미할당, 8087/8097 제안): mongo + redis + rabbit. GradingLogMongoEntity, QuizSessionRedisReadAdapter, GradingCompletedEvent 발행
- stt-service (포트 미할당, 8088/8098 제안): WebSocket 기반 음성→텍스트, DB 미사용 추정(코드로 확인)

[채점 이벤트 체인 — 병목 핵심]
quiz-service → grading-service → RabbitMQ(GradingCompletedEvent) → ranking-service + streak-service (동시 소비)

[메트릭 라벨 규약]
- App 메트릭: 라벨 application=<svc>-service (각 서비스 application.yaml의 management.metrics.tags.application)
- 트레이싱: 라벨 service_name (Tempo span-metrics, service-graph)
- Postgres: 라벨 datname (DB명은 .env.perf의 USER_POSTGRES_DB/TOPIC_/QUIZ_/INTERVIEW_POSTGRES_DB)

[결정 사항]
- 관제 범위: 9개 서비스 전체 (grading·stt 포함 — perf compose/prometheus/대시보드/DB까지)
- PostgreSQL: DB당 postgres_exporter 1개 (postgres-user/topic/quiz/interview 각각 → job=postgres-<svc>)

[핵심 staleness — 현재 깨진 곳]
- ops/prometheus/prometheus.yml(perf): 아직 app:8081 + hellocs-app 잡. 9서비스 미스크랩
- postgres_exporter: postgres-user만 연결. 나머지 3 DB 미수집
- grafana db.json: hikari 패널 application="hellocs" 하드코딩 / datname="hellocs" 하드코딩
- grafana red.json: up{job="hellocs-app"} 잔재
- rules/resource.yml: hikari·tomcat 사용률에 by(application) 미적용
- k6/collect-metrics.ps1: 쿼리 비-application 스코프 + datname="hellocs" 하드코딩
- k6/CLAUDE.md: streak=PostgreSQL로 오기재(실제 MongoDB), grading 체인 미반영
- k6/analyze.ps1: 죽은 참조(docker-compose-local-infra.yaml, claude-opus-4-7). /analyze 스킬은 collect-metrics.ps1을 씀

[CLAUDE.md 준수 규칙 — 반드시]
- 지시받은 작업만. 다음 단계 예단 금지.
- build/compile 테스트 수행 금지(따로 지시 없으면).
- 셸은 PowerShell(pwsh) 사용. 파일 경로 출력은 파일명만(동일명 충돌 시에만 모듈상대경로).
- 새로 생성한 파일은 git add로 추적 시작(.env 같은 로컬 전용 제외).
- 모니터링 스택은 docker-compose.perf.yml, profiles: app / monitoring. 실행은 wsl docker compose.
```

---

## Phase 1 — 현황 인벤토리 (read-only)

> 권장: `Explore` 또는 일반 에이전트, 읽기 전용. 산출물 1개 파일.

```text
[공유 컨텍스트 전체 붙여넣기]

[역할] 너는 관제 인벤토리 조사 에이전트다. 코드/설정을 수정하지 말고, 현재 무엇이
어떻게 수집·소비되는지 사실관계만 확정해 한 개 문서로 정리한다.

[조사 항목 — 각각 파일경로+라인 근거와 함께]
1. 9개 서비스 각각의 application.yaml/application-perf.yaml에서:
   - management.metrics.tags.application 값
   - management.otlp.tracing.endpoint / sampling 유무
   - actuator exposure(prometheus 포함 여부), management 포트
   - 사용 DB(datasource url) / redis / mongo / rabbit 의존
   → grading-service, stt-service는 실제 코드(adapter/out)로 DB·redis·mongo 사용 여부 확정
2. ops/prometheus/prometheus.yml + prometheus.prod.yml: 현재 스크랩 잡 목록과 누락 서비스
3. docker-compose.perf.yml: 정의된 app 서비스 / 익스포터 목록, grading·stt 누락 확인,
   postgres_exporter가 어떤 DB를 가리키는지
4. ops/prometheus/rules/*.yml: 레코딩 룰별 라벨 집계 차원(by 절)
5. grafana json 4개(red/db/resource/trace): 템플릿 변수, 하드코딩된 application=/datname=/job= 값
6. k6/collect-metrics.ps1: PromQL 쿼리별 라벨 스코프, 하드코딩 값
7. RestClient/WebClient 아웃바운드 호출에 micrometer http_client_requests 계측이 잡히는지
   (quiz→user/topic/grading 호출부 코드 확인)
8. RabbitMQ 큐 정의(common-amqp RabbitMQConfig)와 GradingCompletedEvent 발행/소비 지점

[산출물] ops/observability/INVENTORY.md 를 생성하고 git add.
구조:
  ## 1. 서비스별 관제 설정 표 (application 라벨/트레이싱/DB/포트)
  ## 2. 현재 스크랩 토폴로지 (수집 O/X 매트릭스)
  ## 3. 대시보드·룰·스크립트의 staleness 목록 (파일:라인 → 증상)
  ## 4. 계측 갭 (http_client, rabbitmq, per-DB 등 아예 안 잡히는 것)
  ## 5. 미해결/확인필요 질문
숫자·경로는 실제 확인값만. 추측은 "확인필요"로 표기.
끝나면 핵심 5줄 요약을 채팅에 출력.
```

---

## Phase 2 — 타깃 설계 (planning, 코드수정 없음)

```text
[공유 컨텍스트 전체 붙여넣기]

[선행] ops/observability/INVENTORY.md 를 먼저 읽어라. 없으면 Phase 1 미완료이니 중단하고 알려라.

[역할] 너는 관제 설계 에이전트다. 코드를 고치지 말고, "어떻게 나눌지"의 목표 사양을 확정한다.
3대 목적: ① 모듈별 관제 ② 모듈별 흐름·병목 ③ DB별 관제.

[설계 결정 항목]
1. 스크랩 토폴로지(목표): 9개 app 잡(management 포트), postgres 4잡(DB당 exporter),
   mongodb/redis/rabbitmq/cadvisor/node 잡. 각 잡의 relabel로 부여할 라벨(service=, db=) 정의.
   grading/stt의 app포트·management포트 확정(제안: grading 8087/8097, stt 8088/8098).
2. 레코딩 룰(목표): RED는 by(application,uri); 리소스(hikari/tomcat/jvm)는 by(application);
   per-DB는 by(datname). 추가할 룰 식 명시.
3. 대시보드(목표):
   - red.json: $service 변수에 9서비스, up 패널을 잡 기반으로 교체
   - resource.json: 패널 전부 by(application) + cadvisor by(container)
   - db.json: $datname(또는 $db) 변수 추가, 슬로우쿼리/캐시히트/deadlock/temp를 DB별 분리,
     hikari 패널의 application="hellocs"→$service
   - trace.json: service-graph 엣지(rate/error/latency) 패널 확인·보강
   - 신규 messaging.json: RabbitMQ 큐별 depth/publish/deliver/ack/consumer, 채점 이벤트 체인 패널
   - (선택) flow.json 또는 trace.json에 http_client_requests 아웃바운드 레이턴시 패널
4. k6 분석(목표): collect-metrics.ps1이 서비스별 루프 + DB별 datname 루프 + 큐별로 수집하도록
   출력 섹션 구조 재설계(텍스트 스펙만). analyze.md 스킬의 새 섹션 구성.
5. 계측 갭 대응: http_client_requests가 안 잡히면 어디에 ObservationRegistry/RestClient 빌더
   계측을 넣어야 하는지(공통 모듈 common-web 후보) 위치만 지정. (실제 구현은 Phase 3가 함)

[산출물] ops/observability/TARGET-DESIGN.md 생성 + git add.
각 항목을 "현재→목표→변경파일" 형태로. PromQL 식과 라벨 규약은 복붙 가능한 수준으로 구체화.
Phase 3 세 에이전트(3A 스크랩·익스포터 / 3B 룰·대시보드 / 3C k6·분석)가 각자 읽고
독립 실행할 수 있도록 파일별로 작업을 귀속시켜라.
끝나면 3A/3B/3C 각각이 만질 파일 목록을 채팅에 출력.
```

---

## Phase 3A — 스크랩 & 익스포터

```text
[공유 컨텍스트 전체 붙여넣기]

[선행] ops/observability/TARGET-DESIGN.md 의 "1. 스크랩 토폴로지" 섹션을 읽고 그대로 구현.

[작업 파일 — 이 셋만 만진다 (3B/3C 영역 침범 금지)]
1. ops/prometheus/prometheus.yml (perf): 9개 app 잡(management 포트) + postgres 4잡 +
   mongodb/redis/rabbitmq/cadvisor/node 잡으로 재작성. relabel로 service/db 라벨 부여.
   app:8081·hellocs-app 잡 제거.
2. ops/prometheus/prometheus.prod.yml: grading/stt 잡 + postgres 4 exporter 잡 추가(패리티).
3. docker-compose.perf.yml:
   - grading-service, stt-service 컨테이너 추가(빌드 컨텍스트·env·healthcheck·deploy 한도,
     기존 서비스 블록과 동일 패턴). gateway env/depends_on에 두 서비스 배선.
   - postgres-topic/quiz/interview용 postgres_exporter 3개 추가(job명 postgres-<svc>).
     기존 postgres_exporter는 job=postgres-user로 명확화.
   - grading은 mongo/redis/rabbit, stt는 코드 확인 결과 의존만 연결.
4. .env.perf.example: grading/stt의 포트·메모리/CPU 한도·(필요시)DB명 변수 추가.
   .env.perf 로컬 파일은 git add 제외.

[제약] build/compile 금지. compose 문법은 기존 블록 복제로 정합성 유지.
변경 후 "docker compose config" 같은 검증이 필요하면 명령만 제안하고 실행은 사용자에게 맡겨라.
새 파일 git add. 완료 시 변경 파일별 1줄 요약 출력.
```

---

## Phase 3B — 룰 & 대시보드

```text
[공유 컨텍스트 전체 붙여넣기]

[선행] TARGET-DESIGN.md 의 "2. 룰" "3. 대시보드" 섹션을 읽고 구현.

[작업 파일 — 이 영역만]
1. ops/prometheus/rules/resource.yml: hikari·tomcat·jvm 사용률 식에 by(application) 적용.
2. ops/prometheus/rules/red.yml: 현행 유지 검증, 필요한 per-application 보강.
3. ops/grafana/.../json/red.json: up{job="hellocs-app"} → 9잡 기반 헬스 패널로 교체,
   $service 변수에 9서비스 들어오는지 확인.
4. db.json: $db(datname) 템플릿 변수 추가(query: label_values(pg_stat_database_numbackends, datname)),
   슬로우쿼리/buffer-hit/deadlock/temp/xact 패널을 datname=~"$db"로, hikari 패널 application="hellocs"→application=~"$service".
5. resource.json: 모든 패널 application=~"$service", cadvisor 패널 container 기준.
6. trace.json: $service에 9 service_name, service-graph rate/error/latency 엣지 패널 보강.
7. 신규 ops/grafana/.../json/messaging.json: RabbitMQ 큐별 depth/publish/deliver/ack/consumer +
   채점 이벤트 체인(ranking·streak 소비) 패널. dashboard provider가 자동 로드하는 폴더에 둘 것.

[제약] Grafana 11.x 스키마. 기존 json의 패널 구조·datasource uid(prometheus/tempo) 컨벤션 따름.
JSON 유효성 깨지지 않게(에디트 후 구조 확인). 새 파일 git add. 완료 시 패널 변경 요약 출력.
```

---

## Phase 3C — k6 & 분석

```text
[공유 컨텍스트 전체 붙여넣기]

[선행] TARGET-DESIGN.md 의 "4. k6 분석" 섹션을 읽고 구현.

[작업 파일 — 이 영역만]
1. k6/collect-metrics.ps1:
   - 비-application 쿼리를 서비스별로 분해(9서비스 루프, application=~"<svc>-service").
   - PostgreSQL 섹션을 datname="hellocs" 하드코딩 → user/topic/quiz/interview DB명 루프(.env.perf 변수와 일치).
   - RabbitMQ 큐별 + 채점 이벤트 체인 지표 섹션 추가.
   - (계측되면) http_client_requests 아웃바운드 레이턴시 섹션 추가.
   - 출력 섹션을 "모듈별 / 흐름·병목 / DB별" 3블록으로 재구성.
2. .claude/commands/analyze.md: collect-metrics 새 출력에 맞춰 분석 단계 재작성
   (모듈별 → 흐름/병목(service-graph·event chain) → DB별).
3. k6/analyze.ps1: 죽은 참조 수정 — docker-compose-local-infra.yaml → docker-compose.perf.yml,
   모델 id 최신화(claude-opus-4-8). 만약 /analyze 스킬이 collect-metrics만 쓰고 analyze.ps1이
   더 이상 호출되지 않으면 "사용 안 함" 명시 또는 사용자 확인 후 제거.
4. k6/CLAUDE.md: streak=PostgreSQL→MongoDB 정정, grading-service 체인·9서비스·4 PG 인프라표 갱신,
   서비스별 threshold 매핑 보강.

[제약] PowerShell 문법. k6 시나리오(ranking/streak/quiz.js)는 gateway:8080 경유라 라우팅 불변이면
손대지 말 것 — 단, 엔드포인트가 바뀌었으면 보고만. build/실행 테스트 금지(명령 제안만).
새 파일 git add. 완료 시 변경 요약 출력.
```

---

## 실행 순서 요약

1. **Phase 1** 새 세션 → `INVENTORY.md` 확정 (staleness 실제 검증 포함)
2. **Phase 2** 새 세션 → `TARGET-DESIGN.md` (라벨 규약·PromQL·파일귀속 확정)
3. **Phase 3A / 3B / 3C** 각각 새 세션(병렬 가능) → 실제 수정
4. `/perf` → `/analyze` 1회 검증 (사람이 트리거)
