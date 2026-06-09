# HelloCS API 명세서

## 공통 사항

### Base URL

```
http://localhost:8080/api/v1
```

### 공통 응답 형식 (`ApiResponse<T>`)

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `isSuccess` | `boolean` | 요청 성공 여부 |
| `code` | `string` | 응답 코드 |
| `message` | `string` | 응답 메시지 |
| `data` | `T \| null` | 응답 데이터 (실패 시 `null`) |

### 인증

- OAuth2 (Kakao) 로그인 후 발급된 JWT **Bearer Token**을 `Authorization` 헤더에 포함
- Refresh Token은 HTTP-Only 쿠키로 관리

```
Authorization: Bearer {accessToken}
```

### 공통 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `COMMON400` | 400 | 잘못된 요청 |
| `COMMON401` | 401 | 인증 필요 |
| `COMMON403` | 403 | 접근 금지 |
| `COMMON404` | 404 | 리소스 없음 |
| `COMMON500` | 500 | 서버 내부 오류 |
| `USER401` | 401 | 사용자를 찾을 수 없음 |
| `USER409` | 409 | 이미 사용 중인 닉네임 |
| `USER409_1` | 409 | 이미 가입된 사용자 |
| `AUTH401` | 401 | 유효하지 않은 리프레시 토큰 |

### Enum 타입

**QuizMode**

| 값 | 설명 |
|----|------|
| `STANDARD` | 일반 모드 (OX, 객관식, 주관식) |
| `VOICE` | 음성 모드 |

**QuizType**

| 값 | 설명 |
|----|------|
| `OX` | OX 퀴즈 |
| `MULTIPLE_CHOICE` | 객관식 |
| `SHORT_ANSWER` | 주관식 |
| `VOICE` | 음성 |

**QuizLevel**

| 값 | 설명 |
|----|------|
| `JUNIOR` | 초급 |
| `SEMIPRO` | 중급 |
| `PRO` | 고급 |

---

## 1. Auth (인증)

### 1.1 카카오 OAuth2 로그인 리다이렉트

```
GET /api/v1/auth/oauth2/kakao
```

- **인증**: 불필요
- **설명**: 카카오 OAuth2 로그인 페이지로 리다이렉트
- **Response**: `302 Redirect` -> 카카오 로그인 페이지

### 1.2 OAuth2 콜백 (토큰 발급)

```
GET /api/v1/auth/token
```

- **인증**: 불필요
- **설명**: 카카오 OAuth2 콜백 처리 후 JWT 토큰 발급 (Spring Security가 처리)
- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "accessToken": "eyJhbGciOiJI...",
    "isUser": true,
    "userData": {
      "nickname": "홍길동",
      "profileImage": "https://...",
      "kakaoEmail": "user@kakao.com"
    }
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | `string` | JWT Access Token |
| `isUser` | `boolean` | 기존 회원 여부 (`false`면 회원가입 필요) |
| `userData` | `object` | 카카오 사용자 정보 |

### 1.3 토큰 재발급

```
POST /api/v1/auth/reissue
```

- **인증**: 불필요 (쿠키의 Refresh Token 사용)
- **설명**: Refresh Token으로 Access Token 재발급
- **Cookie**: `refreshToken` (HTTP-Only)
- **Response Body**: 1.2와 동일한 `AuthTokenResponse`

---

## 2. User (사용자)

### 2.1 회원가입

```
POST /api/v1/users
```

- **인증**: 필요
- **Request Body**:

```json
{
  "nickname": "홍길동",
  "kakaoEmail": "user@kakao.com",
  "profileImage": "https://...",
  "quizLevel": "JUNIOR",
  "interests": [1, 2, 3]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `nickname` | `string` | O | 닉네임 |
| `kakaoEmail` | `string` | O | 카카오 이메일 |
| `profileImage` | `string` | O | 프로필 이미지 URL |
| `quizLevel` | `QuizLevel` | O | 퀴즈 난이도 (`JUNIOR`, `SEMIPRO`, `PRO`) |
| `interests` | `Long[]` | O | 관심 주제 ID 목록 |

- **Response**: `ApiResponse<Void>`

### 2.2 내 프로필 조회

```
GET /api/v1/users/me
```

- **인증**: 필요
- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "profileImage": "https://...",
    "nickname": "홍길동",
    "interests": ["운영체제", "네트워크", "데이터베이스"]
  }
}
```

### 2.3 내 프로필 수정

```
PATCH /api/v1/users/me
```

- **인증**: 필요
- **Request Body**:

```json
{
  "nickname": "새닉네임",
  "profileImage": "https://...",
  "interestTopicIds": [1, 4, 5]
}
```

- **Response**: `ApiResponse<Void>`

### 2.4 회원 탈퇴

```
DELETE /api/v1/users/me
```

- **인증**: 필요
- **Response**: `ApiResponse<Void>`

---

## 3. Topic (주제)

### 3.1 전체 주제 목록 조회

```
GET /api/v1/topics
```

- **인증**: 불필요
- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": [
    { "id": 1, "name": "운영체제" },
    { "id": 2, "name": "네트워크" },
    { "id": 3, "name": "데이터베이스" }
  ]
}
```

---

## 4. Quiz (퀴즈)

### 4.1 퀴즈 조회

```
POST /api/v1/quiz
```

- **인증**: 필요
- **설명**: 주제와 모드에 따라 퀴즈를 조회
- **Request Body**:

```json
{
  "topicIds": [1, 2],
  "mode": "STANDARD"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `topicIds` | `Long[]` | O | 주제 ID 목록 |
| `mode` | `QuizMode` | O | 퀴즈 모드 (`STANDARD`, `VOICE`) |

- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "oxQuizzes": [
      { "id": 1, "content": "TCP는 연결 지향 프로토콜이다." }
    ],
    "multipleChoiceQuizzes": [
      {
        "id": 2,
        "content": "다음 중 OSI 7계층에 해당하지 않는 것은?",
        "choices": ["물리 계층", "데이터 계층", "컴파일 계층", "전송 계층"]
      }
    ],
    "shortAnswerQuizzes": [
      { "id": 3, "content": "프로세스와 스레드의 차이를 설명하시오." }
    ],
    "voiceQuizzes": [
      { "id": 4, "content": "audio_url", "contentText": "HTTP와 HTTPS의 차이를 설명하시오." }
    ]
  }
}
```

> `mode=STANDARD`일 경우 `voiceQuizzes`는 빈 배열, `mode=VOICE`일 경우 `oxQuizzes`, `multipleChoiceQuizzes`, `shortAnswerQuizzes`는 빈 배열

---

## 5. Grading (채점)

### 5.1 답안 제출 (채점 요청)

```
POST /api/v1/quiz/grading
```

- **인증**: 필요
- **Request Body**:

```json
[
  { "quizId": 1, "answer": "O" },
  { "quizId": 2, "answer": "3" },
  { "quizId": 3, "answer": "프로세스는 독립적인 메모리 공간을 가지며..." }
]
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `quizId` | `Long` | O | 퀴즈 ID |
| `answer` | `string` | O | 사용자 답변 |

- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "gradingLogId": "abc123-def456"
  }
}
```

### 5.2 채점 결과 조회

```
GET /api/v1/quiz/grading/{gradingLogId}
```

- **인증**: 필요
- **Path Parameter**: `gradingLogId` (string)
- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "correctCount": 2,
    "quizCount": 3,
    "gradingResults": [
      {
        "quizId": 1,
        "isCorrect": true,
        "score": 100
      },
      {
        "quizId": 2,
        "isCorrect": true,
        "score": 100
      },
      {
        "quizId": 3,
        "isCorrect": false,
        "score": 60
      }
    ]
  }
}
```

### 5.3 채점 상세 결과 조회

```
GET /api/v1/quiz/grading/{gradingLogId}/{quizId}
```

- **인증**: 필요
- **Path Parameters**: `gradingLogId` (string), `quizId` (Long)
- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "quizId": 3,
    "score": 60,
    "isCorrect": false,
    "content": "프로세스와 스레드의 차이를 설명하시오.",
    "quizType": "SHORT_ANSWER",
    "userAnswer": "프로세스는 독립적인 메모리 공간을 가지며...",
    "correctAnswer": "프로세스는 독립된 메모리 공간을 가지고...",
    "feedback": "스레드의 공유 자원에 대한 설명이 부족합니다.",
    "missingKeywords": ["공유 메모리", "컨텍스트 스위칭"],
    "improvedAnswer": "프로세스는 독립된 메모리 공간을 가지며, 스레드는..."
  }
}
```

### 5.4 채점 기록 목록 조회

```
GET /api/v1/quiz/grading/list
```

- **인증**: 필요
- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": [
    {
      "id": "abc123-def456",
      "solvedAt": "2025-01-15T14:30:00",
      "correctCount": 2,
      "totalCount": 3,
      "quizMode": "STANDARD",
      "topicNames": ["운영체제", "네트워크"]
    }
  ]
}
```

---

## 6. Ranking (랭킹)

### 6.1 랭킹 요약 (Top 5)

```
GET /api/v1/ranking/summary
```

- **인증**: 불필요
- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "top5": [
      {
        "rank": 1,
        "nickname": "홍길동",
        "profileImage": "https://...",
        "score": 950
      }
    ],
    "recentRelatedDiscussionCount": 42
  }
}
```

### 6.2 랭킹 상세 조회

```
GET /api/v1/ranking?filterType=ALL&size=10
```

- **인증**: 필요
- **Query Parameters**:

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `filterType` | `string` | `ALL` | `ALL`: 전체 랭킹, `INTEREST`: 내 관심 주제 기준 |
| `size` | `int` | `10` | 조회할 랭킹 수 (1~100) |

- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "filterType": "ALL",
    "rankings": [
      {
        "rank": 1,
        "nickname": "홍길동",
        "profileImage": "https://...",
        "score": 950
      }
    ],
    "myRanking": {
      "rank": 15,
      "nickname": "나",
      "profileImage": "https://...",
      "score": 500
    },
    "belowMyRankings": [
      {
        "rank": 16,
        "nickname": "사용자16",
        "profileImage": "https://...",
        "score": 490
      }
    ],
    "recentRelatedDiscussionCount": 42
  }
}
```

---

## 7. Streak (스트릭)

### 7.1 스트릭 요약 조회

```
GET /api/v1/streak
```

- **인증**: 필요
- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "currentStreakDays": 7,
    "solvedQuizCount": 42,
    "solvedTopicCount": 5
  }
}
```

### 7.2 월별 스트릭 조회

```
GET /api/v1/streak?year=2025&month=12
```

- **인증**: 필요
- **Query Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `year` | `int` | O | 연도 (>= 2000) |
| `month` | `int` | O | 월 (1~12) |

- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "year": 2025,
    "month": 12,
    "days": [
      {
        "date": "2025-12-01",
        "solved": true,
        "quizCount": 3
      }
    ]
  }
}
```

### 7.3 스트릭 상세 통계 조회

```
GET /api/v1/streak/detail
```

- **인증**: 필요
- **Response Body**:

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "data": {
    "currentStreakDays": 7,
    "solvedQuizCount": 42,
    "solvedTopicCount": 5,
    "longestStreakDays": 15,
    "lastSolvedDate": "2025-01-15",
    "solvedToday": true,
    "activeDaysThisMonth": 12,
    "currentMonthSolvedQuizCount": 30
  }
}
```

---

## CORS 설정

| 항목 | 값 |
|------|-----|
| Allowed Origins | `http://localhost:5173`, `https://localhost:5173`, `https://hellocs.site` |
| Allowed Methods | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Credentials | `true` |
