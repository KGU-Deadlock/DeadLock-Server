# k6 부하테스트

## 목적

개선 전/후 성능 변화를 측정합니다.
RPS(초당 요청 수)를 낮은 값에서 목표치까지 점진적으로 올리면서
**어느 RPS에서 응답시간이 올라가기 시작하는지** 한계 지점을 찾습니다.

```
RPS  5 → ... → 200  (3분 30초)
             ↑
      여기서 응답시간이 튀면 그게 현재 한계
      개선 후 같은 테스트에서 한계가 올라갔으면 성공
```

---

## 사전 준비

### 1. k6 설치
```pwsh
winget install k6
```

### 2. Docker Desktop 실행 확인

모든 인프라(DB, 모니터링, WireMock)와 앱 서버가 Docker로 실행됩니다.
Docker Desktop이 실행 중인지 확인하세요.

---

## 워크플로 (2단계)

### Step 1: 데이터셋 Bake (최초 1회 또는 데이터 변경 시)

MSA 스택(게이트웨이 + 9개 서비스)을 기동하고 시드 데이터를 생성한 뒤
서비스별 Postgres(×4) + Redis + MongoDB를 각각 분리 덤프합니다.

```pwsh
# 기본 데이터셋 생성 (100명, 30일)
.\k6\bake-dataset.ps1 -Name default

# 소규모 데이터셋 생성
.\k6\bake-dataset.ps1 -Name small -UserCount 50 -Days 14

# 대규모 데이터셋 생성
.\k6\bake-dataset.ps1 -Name large -UserCount 500 -Days 60
```

생성된 파일: `ops/perf/datasets/<Name>/`

| 파일 | 내용 |
|---|---|
| `postgres-user.sql` | user-service DB (users, user_interest) |
| `postgres-topic.sql` | topic-service DB (topics) |
| `postgres-quiz.sql` | quiz-service DB (quiz, quiz_*, quiz_category) |
| `postgres-interview.sql` | interview-service DB (interviews, interview_*, cs_questions) |
| `redis/dump.rdb` | ranking ZSet (ranking-service) |
| `mongo/hellocs/` | streak·quiz·grading 문서 |
| `meta.json` | 생성 파라미터 기록 |

> **이벤트 드레인 포함**: `seedStats` 호출 후 RabbitMQ 채점 이벤트가
> ranking-service(Redis) · streak-service(MongoDB)에 비동기 소비될 때까지
> 자동으로 대기한 뒤 덤프합니다.

### Step 2: 부하테스트 실행 (반복 실행)

```pwsh
# 기본 실행 (모든 시나리오)
.\k6\run.ps1 -Dataset default

# 특정 시나리오만
.\k6\run.ps1 -Dataset default -Scenario quiz

# 소규모 데이터셋으로 낮은 RPS 테스트
.\k6\run.ps1 -Dataset small -RpsRanking 50 -RpsStreak 30 -RpsQuiz 15
```

실행 시 자동으로:
1. 스택 초기화 (`docker compose down -v`) → 선택된 데이터셋으로 모든 컨테이너 시작
2. 서비스별 Postgres가 자기 덤프 파일(`postgres-*.sql`)을 각각 적재
3. k6 시나리오 실행 (시드 단계 없이 즉시 시작)
4. 테스트 종료 후 스택 종료

---

## run.ps1 파라미터

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `-Dataset` | `"default"` | 사용할 데이터셋 이름 |
| `-Scenario` | `"all"` | `all` / `ranking` / `streak` / `quiz` |
| `-RpsRanking` | `200` | 랭킹 목표 최대 RPS |
| `-RpsStreak` | `100` | 스트릭 목표 최대 RPS |
| `-RpsQuiz` | `50` | 퀴즈 목표 최대 RPS |
| `-MaxVus` | `500` | VU 상한 |
| `-Gui` | `"grafana"` | `grafana` / `web` / `none` |
| `-KeepUp` | `false` | 테스트 후 스택 유지 |
| `-Debug` | `false` | 앱 로그 출력 |

노트북 성능이 부족하면 RPS를 낮추세요:
```pwsh
.\k6\run.ps1 -Dataset default -RpsRanking 50 -RpsStreak 30 -RpsQuiz 15
```

## bake-dataset.ps1 파라미터

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `-Name` | 필수 | 데이터셋 이름 |
| `-UserCount` | `100` | 시드 유저 수 |
| `-Days` | `30` | 유저당 과거 데이터 일수 |
| `-QuizPerCombo` | `5` | (토픽 × 레벨 × 유형) 조합당 퀴즈 수 |
| `-CsPerCategory` | `10` | CS 면접 문제 카테고리당 문항 수 |

---

## GUI 관찰

### Grafana 실시간 대시보드 (기본)

```
http://localhost:3000  (admin / admin)
→ Dashboards → k6 부하테스트
```

### k6 내장 Web Dashboard

```pwsh
.\k6\run.ps1 -Dataset default -Gui web
```
```
http://localhost:5665
```

---

## 결과 해석

### 한계 지점 찾기

Grafana 또는 k6 대시보드에서 **응답시간 그래프**를 보면:

```
응답시간
    |                          ↗ 여기서 급격히 올라가는 RPS = 한계
    |                     ↗
    |___________________↗
    +---------------------------→ RPS (시간)
```

### 주요 메트릭

| 메트릭 | 의미 |
|--------|------|
| `http_req_duration p95` | 요청의 95%가 이 시간 안에 완료 |
| `http_req_failed rate` | 에러율 (5% 초과 시 임계값 위반) |
| `dropped_iterations` | k6가 목표 RPS를 못 맞춰서 버린 요청 수 → 서버 과부하 신호 |

### 병목 분석

```pwsh
# PostgreSQL 슬로우 쿼리 (quiz DB 예)
wsl docker compose --env-file .env.perf -f docker-compose.perf.yml exec -T postgres-quiz `
  psql -U hellocs -d quiz_db -c "
    SELECT LEFT(query, 80), calls,
           ROUND(mean_exec_time::numeric, 2) AS avg_ms
    FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"
```

---

## 인프라 구성 (MSA, 9개 서비스)

k6 시나리오는 **gateway(8080)** 만 호출합니다.

| 서비스 | app 포트 | mgmt 포트 | 사용 인프라 |
|---|---|---|---|
| gateway | 8080 | 8081 | 전 서비스 라우팅 (Netty/reactive) |
| user-service | 8081 | 8091 | PostgreSQL(user_db) + RabbitMQ |
| topic-service | 8082 | 8092 | PostgreSQL(topic_db) |
| quiz-service | 8083 | 8093 | PostgreSQL(quiz_db) + Redis + MongoDB + RabbitMQ |
| interview-service | 8084 | 8094 | PostgreSQL(interview_db) + RabbitMQ |
| ranking-service | 8085 | 8095 | Redis + RabbitMQ |
| streak-service | 8086 | 8096 | MongoDB + RabbitMQ |
| grading-service | 8087 | 8097 | Redis + MongoDB + RabbitMQ |
| stt-service | 8088 | 8098 | WebSocket (음성→텍스트) |
| dev-service | 8090 | 8099 | 시딩 전용 (bake 시에만 사용) |
| WireMock | 8089 | — | AI 채점 API 목 |

JVM 설정: 각 서비스 `-Xms512m -Xmx512m -XX:ActiveProcessorCount=2` (`.env.perf` 의 `<SVC>_MEMORY_LIMIT` / `<SVC>_CPU_LIMIT` 로 제어)

---

## 응답 검증 정책

모든 시나리오는 `lib/validators.js`의 `validateXxx()` 함수를 통해 3단계로 응답을 검증합니다.

| 단계 | 내용 | 예시 |
|---|---|---|
| 1. 상태 & envelope | `status === 200`, `isSuccess === true`, `data` 존재 | 모든 엔드포인트 |
| 2. 타입 & 범위 | 필드 타입(`string/number/boolean/array`)과 합리적 범위 | `rank >= 1`, `score 0~100`, `days 배열` |
| 3. Mock 고정값 | WireMock 목 응답의 고정 기댓값 확인 | AI 채점 `score === 80`, `feedback` 문자열 일치 |

---

## 파일 구조

```
k6/
├── run.ps1              ← 부하테스트 실행 (Dataset, Scenario, RPS 등 파라미터)
├── bake-dataset.ps1     ← 데이터셋 생성 (MSA 스택 기동·시딩·서비스별 분리 덤프)
├── lib/
│   ├── auth.js          # 토큰 풀 생성
│   ├── config.js        # 환경변수 → 설정값 변환
│   ├── init.js          # WireMock 체크
│   └── validators.js    # 응답 검증 헬퍼 (3단계 정책)
├── scenarios/
│   ├── ranking.js
│   ├── streak.js
│   └── quiz.js
├── all.js               # 3개 동시 실행
└── results/             # 테스트 결과 저장 (gitignore)
```
