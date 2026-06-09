export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const WIREMOCK_URL = __ENV.WIREMOCK_URL || 'http://localhost:8089';

// 시나리오별 목표 최대 RPS (초당 요청 수)
// k6가 이 RPS를 달성하도록 VU를 자동으로 늘림
export const MAX_RPS_RANKING = parseInt(__ENV.MAX_RPS_RANKING) || 200;
export const MAX_RPS_STREAK = parseInt(__ENV.MAX_RPS_STREAK) || 100;
export const MAX_RPS_QUIZ = parseInt(__ENV.MAX_RPS_QUIZ) || 50;

// VU 상한 (안전장치 — k6가 이 수를 넘게 VU를 만들지 않음)
export const MAX_VUS = parseInt(__ENV.MAX_VUS) || 500;

// 시드 데이터 설정
export const SEED_USER_COUNT = parseInt(__ENV.SEED_USER_COUNT) || 200;
export const SEED_DAYS = parseInt(__ENV.SEED_DAYS) || 30;
export const SEED_QUIZ_PER_COMBO = parseInt(__ENV.SEED_QUIZ_PER_COMBO) || 5;

// setup() 타임아웃 (시드 200명 × 30일 이벤트 + 토큰 풀 200개 생성에 시간이 걸림)
export const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '5m';

// 스킵 옵션 (true 문자열로 전달)
export const SKIP_SEED = __ENV.SKIP_SEED === 'true';
export const SKIP_WIREMOCK_CHECK = __ENV.SKIP_WIREMOCK_CHECK === 'true';

export const DEBUG = __ENV.DEBUG === 'true';

export const QUIZ_THRESHOLDS = {
  http_req_failed: ['rate<0.05'],
  'http_req_duration{name:quiz_fetch}': ['p(95)<1000'],
  'http_req_duration{name:quiz_grading}': ['p(95)<3000'],
};

export const DEFAULT_HEADERS = (token) => ({
  'Content-Type': 'application/json',
  Authorization: `Bearer ${token}`,
});
