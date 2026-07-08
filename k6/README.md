# k6 부하테스트

## 목적

개선 전/후 성능 변화를 측정합니다.
`TARGET_RPS` 를 낮은 값에서 목표치까지 점진적으로 올리면서
**어느 RPS에서 응답시간이 급등하는지** 한계 지점을 찾습니다.

---

## 사전 준비

```pwsh
# k6 설치
winget install k6

# Docker Desktop 실행 확인 (모든 인프라·앱 서버가 Docker로 동작)
```

---

## 워크플로 (2단계)

### Step 1: 데이터셋 Bake (최초 1회 또는 데이터 변경 시)

`ops/perf/profiles/dataset.env` 에 정의된 세그먼트 분포 모델(power/regular/casual)대로
유저·채점기록·스트릭·랭킹을 시딩하고, Postgres(×3) + Redis + MongoDB 를 덤프합니다.

```pwsh
.\k6\bake-dataset.ps1 -Name default
```

생성 파일 (`ops/perf/datasets/<Name>/`):

| 파일 | 내용 |
|---|---|
| `postgres-user.sql` | user-service DB |
| `postgres-topic.sql` | topic-service DB |
| `postgres-quiz.sql` | quiz-service DB |
| `redis/dump.rdb` | ranking ZSet (ranking-service) |
| `mongo/hellocs/` | streak·grading 문서 |
| `meta.json` | 생성 파라미터 (users, segShares, signupWindowDays, quizPerDay, …) |

### Step 2: 부하테스트 실행 (반복 실행)

RPS 는 단일 목표값이 아니라 **계단식 램프**입니다.
`StartRps → EndRps` 를 `StepRps` 단위로 올리며 각 스텝을 `StepDuration` 유지 →
어느 RPS 에서 응답시간이 급등하는지로 한계 지점을 찾습니다.

```pwsh
# 기본 실행 (12개 엔드포인트 전체, 10→100 RPS, step 10, 2m/step)
.\k6\run.ps1 -Dataset default

# 100→500 RPS, 50 단위 램프
.\k6\run.ps1 -Dataset default -StartRps 100 -EndRps 500 -StepRps 50

# grading 모듈만 (격리 모드: grading·비율 재정규화, EndRps 가 grading 전체 RPS)
.\k6\run.ps1 -Dataset default -Module grading -EndRps 200

# 재실행 — 스택 초기화 생략
.\k6\run.ps1 -Dataset default -SkipInit
```

실행 시 자동으로:
1. 스택 초기화 (`compose down -v`) → 선택된 데이터셋으로 모든 컨테이너 시작
2. 서비스별 Postgres가 덤프 파일(`postgres-*.sql`)을 각각 적재
3. k6 시나리오 실행
4. 테스트 종료 후 스택 종료

---

## run.ps1 파라미터

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `-Dataset` | `"default"` | 사용할 데이터셋 이름 |
| `-Module` | `"all"` | `all` / `streak` / `ranking` / `quiz` / `grading` / `user` / `topic` (단일 지정 시 격리 모드) |
| `-StartRps` | `10` | 램프 시작 RPS |
| `-EndRps` | `100` | 램프 최대 RPS (한계 탐색 상한) |
| `-StepRps` | `10` | 스텝당 RPS 증가폭 |
| `-StepDuration` | `"2m"` | 각 스텝 유지 시간 |
| `-MaxVus` | `500` | VU 상한 |
| `-HardwareProfile` | `ops/perf/profiles/hardware.env` | 서비스별 CPU/메모리 한도 |
| `-TargetHost` | `localhost` | 대상 스택 호스트 (외부 서버 시 IP, 또는 `$env:PERF_TARGET_HOST`) |
| `-Gui` | `"grafana"` | `grafana` / `web` (포트 5665) / `none` (CI) |
| `-KeepUp` | `false` | 테스트 후 스택 유지 |
| `-SkipInit` | `false` | 스택 초기화(down -v → up) 없이 재실행 |
| `-Build` | `false` | Gradle bootJar 후 이미지 재빌드 |
| `-Clean` | `false` | Prometheus·Grafana 수집 데이터 초기화 |
| `-VerifyOnly` | `false` | 응답 검증만 수행 (부하 없음) |
| `-Debug` | `false` | 앱 컨테이너 로그 출력 |

> **격리 모드** (`-Module` 단일 지정): 해당 모듈 엔드포인트 비율을 합 100 으로 재정규화하여
> `StartRps~EndRps` 가 그 모듈 전체 RPS 가 되고, 해당 모듈(+전용 DB)에만 hardware.env 한도를
> 적용하며 나머지 컨테이너는 무제한(0)으로 둡니다. 단일 모듈 성능을 격리 측정할 때 사용합니다.

## bake-dataset.ps1 파라미터

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `-Name` | 필수 | 데이터셋 이름 |
| `-Build` | `false` | bootJar 재빌드 후 스택 시작 |
| `-KeepUp` | `false` | 완료 후 bake 스택 유지 (기본: 완료 시 down -v) |
| `-DumpOnly` | `false` | 실행 중인 스택에서 덤프만 수행 |
| `-ShowOutput` | `false` | compose 명령 출력 표시 (평시 억제) |

> 규모·세그먼트 파라미터(users, dpw, share 등)는 **`ops/perf/profiles/dataset.env`** 에서 제어.

---

## 데이터셋 세그먼트 모델 (`ops/perf/profiles/dataset.env`)

```
DATASET_USERS=10000          # 전체 유저 수 (단일 노브 — 비율 유지)
DATASET_SIGNUP_WINDOW_DAYS=180  # 가입일을 0~180일 범위로 균등 분산
DATASET_QUIZ_PER_DAY=30      # 활동일당 풀이 문항 수

SEG_POWER_SHARE=0.2          # power 유저 비율 (주 7일 활동)
SEG_REGULAR_SHARE=0.5        # regular 유저 비율 (주 4일 활동)
SEG_CASUAL_SHARE=0.3         # casual 유저 비율 (주 2일 활동)
```

kakaoId 블록 순서(power→regular→casual)가 k6 토큰풀 선택과 자동으로 일치합니다.

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

Grafana 에서 **응답시간 그래프**를 확인합니다:

```
응답시간
    |                          ↗ 여기서 급등하는 RPS = 한계
    |                     ↗
    |___________________↗
    +---------------------------→ RPS (시간)
```

### 주요 메트릭

| 메트릭 | 의미 |
|---|---|
| `dropped_iterations` 급증 | k6가 목표 RPS를 못 맞춤 → 서버 포화 신호 |
| `http_req_duration p95` 급등 | 병목 진입 직전 |
| `http_req_failed rate > 1%` | 전역 에러율 임계값 위반 |

### 엔드포인트 SLO (threshold 태그)

| 태그 | p95 기준 |
|---|---|
| `streak_summary` | 300ms |
| `streak_monthly` | 1000ms |
| `streak_detail` | 1000ms |
| `ranking_summary` | 300ms |
| `ranking_page` | 1000ms |
| `quiz_fetch` | 3000ms |
| `grading_submit` | 10000ms |
| `grading_result` | 500ms |
| `grading_detail` | 500ms |
| `user_me` | 300ms |
| `topics` | 200ms |

---

## 인프라 구성 (baseline, `--profile app`)

k6 시나리오는 **gateway(8080)** 만 호출합니다.

| 서비스 | app 포트 | mgmt 포트 | 사용 인프라 |
|---|---|---|---|
| gateway | 8080 | 8081 | 전 서비스 라우팅 (Netty/reactive) |
| user-service | 8081 | 8091 | PostgreSQL(user_db) + RabbitMQ |
| topic-service | 8082 | 8092 | PostgreSQL(topic_db) |
| quiz-service | 8083 | 8093 | PostgreSQL(quiz_db) + Redis + MongoDB + RabbitMQ |
| ranking-service | 8085 | 8095 | Redis + RabbitMQ |
| streak-service | 8086 | 8096 | MongoDB + RabbitMQ |
| grading-service | 8087 | 8097 | Redis + MongoDB + RabbitMQ |
| dev-service | — | 8099 | 토큰 발급(/v1/dev/user-token). SUT 예산 외. |
| WireMock | 8089 | — | AI 채점 API 목 |

> `interview-service` / `stt-service` 는 `extra` 프로파일 — baseline 미가동.

리소스 한도: `ops/perf/profiles/hardware.env`

---

## 파일 구조

```
k6/
├── run.ps1              ← 부하테스트 실행 (원격 docker 스택 기동 + k6)
├── bake-dataset.ps1     ← 데이터셋 생성 (dataset.env 읽어 세그먼트 시딩·덤프)
├── collect-metrics.ps1  ← 결과 수집 (k6 summary + Prometheus, /analyze 용)
├── all.js               ← 12개 엔드포인트 통합 실행 (MODULE 필터로 격리)
├── lib/
│   ├── auth.js          # 토큰 풀 생성 (세그먼트별 kakaoId 블록 슬라이싱)
│   ├── config.js        # 환경변수(START/END/STEP_RPS 등) → 설정값
│   ├── endpoints.js     # 12개 엔드포인트 레지스트리
│   ├── init.js          # WireMock 체크·AI 라우팅 검증
│   ├── profile.js       # profile.json(비율·토큰풀) 동적 구성
│   └── validators.js    # 응답 검증 헬퍼
├── .perf-profile.json   # run.ps1 이 생성 (endpoints/tokenPool/dataset) — all.js 가 읽음
└── results/             # 테스트 결과 저장 (gitignore)
```

> 부하 스택은 `wsl docker compose` 로 활성 docker 컨텍스트(원격 `ssh://perf-server` 또는 로컬)에
> 기동됩니다. 데이터셋은 원격 상주 named volume `hellocs-perf-datasets` 에서 읽습니다.
