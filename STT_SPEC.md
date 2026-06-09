# HelloCS STT (Speech-to-Text) 명세서

## 개요

음성 퀴즈 모드에서 사용자의 음성 답변을 텍스트로 변환하는 실시간 STT 시스템입니다.
STOMP 기반 WebSocket을 사용하여 오디오 청크를 스트리밍 방식으로 전송하고, 변환된 텍스트를 실시간으로 수신합니다.

---

## WebSocket 연결 정보

### STOMP 엔드포인트

```
ws://localhost:8080/api/v1/ws
```

### 프로토콜

- **STOMP over WebSocket**
- **Message Broker**: Simple Broker
- **메시지 발행 prefix**: `/v1/ws/pub`
- **구독 prefix**: `/v1/ws/sub`

### 연결 제한

| 항목 | 값 |
|------|-----|
| 메시지 최대 크기 | 512 KB |
| 전송 버퍼 크기 | 512 KB |
| 전송 시간 제한 | 20초 |

---

## 클라이언트 연결 흐름

### 1. WebSocket 연결

```javascript
const socket = new SockJS('/api/v1/ws');
const stompClient = Stomp.over(socket);
stompClient.connect({}, onConnected, onError);
```

### 2. 결과 구독

STT 변환 결과를 수신하기 위해 세션별 토픽을 구독합니다.

```
구독 경로: /v1/ws/sub/stt/{sessionId}
```

```javascript
stompClient.subscribe('/v1/ws/sub/stt/' + sessionId, (message) => {
  const result = JSON.parse(message.body);
  console.log(result.text);  // 변환된 텍스트
});
```

### 3. 오디오 청크 전송

오디오 데이터를 Base64로 인코딩하여 청크 단위로 전송합니다.

```
발행 경로: /v1/ws/pub/stt/chunk
```

```javascript
stompClient.send('/v1/ws/pub/stt/chunk', {}, JSON.stringify({
  sessionId: "uuid-session-id",
  sequence: 0,
  audioBase64: "UklGRi4AAABXQVZFZm10IBAA...",
  isFinalChunk: false
}));
```

---

## 메시지 포맷

### 요청: `SttChunkCommand`

오디오 청크를 서버에 전송할 때 사용하는 메시지 형식입니다.

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "sequence": 0,
  "audioBase64": "UklGRi4AAABXQVZFZm10IBAA...",
  "isFinalChunk": false
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `sessionId` | `string` | O | 세션 식별자 (클라이언트에서 UUID 생성) |
| `sequence` | `long` | O | 청크 순서 번호 (0부터 시작, >= 0) |
| `audioBase64` | `string` | O | Base64 인코딩된 오디오 데이터 |
| `isFinalChunk` | `boolean` | O | 마지막 청크 여부 |

#### `audioBase64` 형식

다음 두 가지 형식을 모두 지원합니다:

- **순수 Base64**: `UklGRi4AAABXQVZFZm10IBAA...`
- **Data URI**: `data:audio/wav;base64,UklGRi4AAABXQVZFZm10IBAA...`

> Data URI 형식의 경우 `base64,` 이후 부분만 자동으로 추출됩니다.

#### 오디오 파일 형식

- **포맷**: WAV
- **언어**: 한국어 (`ko`) 고정

---

### 응답: `SttChunkResult`

서버에서 구독 토픽으로 전송하는 변환 결과입니다.

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "sequence": 0,
  "text": "프로세스는 독립적인 메모리 공간을 가지며",
  "isFinalChunk": false
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `sessionId` | `string` | 요청과 동일한 세션 ID |
| `sequence` | `long` | 요청과 동일한 청크 순서 번호 |
| `text` | `string` | 변환된 텍스트 |
| `isFinalChunk` | `boolean` | 마지막 청크 여부 |

---

## 전체 통신 흐름

```
클라이언트                                    서버
  │                                           │
  │── WebSocket 연결 (/api/v1/ws) ──────────>│
  │                                           │
  │── SUBSCRIBE /v1/ws/sub/stt/{sessionId} ─>│
  │                                           │
  │── SEND /v1/ws/pub/stt/chunk ───────────> │
  │   { sequence: 0, isFinalChunk: false }    │
  │                                           │── AI STT 서버로 전송
  │<── MESSAGE /v1/ws/sub/stt/{sessionId} ── │
  │   { sequence: 0, text: "프로세스는..." }   │
  │                                           │
  │── SEND /v1/ws/pub/stt/chunk ───────────> │
  │   { sequence: 1, isFinalChunk: false }    │
  │                                           │── AI STT 서버로 전송
  │<── MESSAGE /v1/ws/sub/stt/{sessionId} ── │
  │   { sequence: 1, text: "독립적인..." }     │
  │                                           │
  │── SEND /v1/ws/pub/stt/chunk ───────────> │
  │   { sequence: 2, isFinalChunk: true }     │
  │                                           │── AI STT 서버로 전송
  │<── MESSAGE /v1/ws/sub/stt/{sessionId} ── │
  │   { sequence: 2, text: "가집니다.",        │
  │     isFinalChunk: true }                  │
  │                                           │
```

---

## 아키텍처

```
┌─────────────────────────────────────────────────────┐
│  quiz/stt 모듈                                       │
│                                                      │
│  ┌──────────────────┐    ┌────────────────────────┐  │
│  │ SttWebSocket     │───>│ CommandSttStream       │  │
│  │ Controller       │    │ InputPort (인바운드)     │  │
│  │ (WebSocket 어댑터) │    └──────────┬─────────────┘  │
│  └──────────────────┘               │                │
│                                     v                │
│                          ┌────────────────────────┐  │
│                          │ SttStreamService       │  │
│                          │ (비즈니스 로직)          │  │
│                          └──────────┬─────────────┘  │
│                                     │                │
│                                     v                │
│                          ┌────────────────────────┐  │
│                          │ CommandSttAi           │  │
│                          │ OutputPort (아웃바운드)   │  │
│                          └──────────┬─────────────┘  │
│                                     │                │
│  ┌──────────────────┐               │                │
│  │ AiSttAdapter     │<──────────────┘                │
│  │ (외부 AI 어댑터)   │                                │
│  └──────┬───────────┘                                │
│         │                                            │
└─────────┼────────────────────────────────────────────┘
          │
          v
  ┌──────────────────┐
  │  외부 AI STT 서버  │
  │  (Whisper 등)     │
  └──────────────────┘
```

### 외부 AI STT 서버 연동

- **엔드포인트**: `ai.stt.transcribe-endpoint` (application 설정)
- **요청 형식**: `multipart/form-data`

| 파라미터 | 값 | 설명 |
|----------|-----|------|
| `file` | `audio.wav` | 디코딩된 오디오 파일 |
| `language` | `ko` | 한국어 고정 |
| `task` | `transcribe` | 음성 인식 작업 |

- **응답 형식**:

```json
{
  "text": "변환된 텍스트"
}
```

---

## 클라이언트 구현 가이드

### 1. 세션 ID 생성

각 음성 퀴즈 응답 시 고유한 `sessionId`를 생성합니다 (UUID 권장).

### 2. 오디오 녹음 및 청크 분할

- Web Audio API 또는 MediaRecorder API로 녹음
- WAV 포맷으로 변환
- 적절한 크기로 분할 (512KB 이하 권장)
- 각 청크를 Base64로 인코딩

### 3. 순서 보장

- `sequence`를 0부터 순차적으로 증가시켜 전송
- 마지막 청크에 `isFinalChunk: true` 설정

### 4. 결과 수집

- 구독 토픽에서 각 청크별 변환 텍스트 수신
- `sequence` 순서대로 텍스트를 결합하여 최종 답변 완성
- `isFinalChunk: true` 수신 시 전체 변환 완료
