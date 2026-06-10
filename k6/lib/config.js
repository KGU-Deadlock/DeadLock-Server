export const BASE_URL     = __ENV.BASE_URL     || 'http://localhost:8080';
export const WIREMOCK_URL = __ENV.WIREMOCK_URL || 'http://localhost:8089';

// ── 단일 RPS 노브 ─────────────────────────────────────────────────
// 전체 TARGET_RPS를 api.env 비율(RATIO_*)대로 12개 시나리오에 배분.
export const TARGET_RPS = parseInt(__ENV.TARGET_RPS) || 100;

// 실행할 모듈 필터: all | streak | ranking | quiz | grading | user | topic
// (step 2 모듈별 측정 시 사용. 비율은 그대로 유지됨.)
export const MODULE = __ENV.MODULE || 'all';

// VU 상한 (안전장치)
export const MAX_VUS = parseInt(__ENV.MAX_VUS) || 500;

// ── 토큰 풀 (profile.json 없을 때 fallback) ───────────────────────
export const TOKEN_POOL_SIZE = parseInt(__ENV.TOKEN_POOL_SIZE) || 200;
export const DATASET_USERS   = parseInt(__ENV.DATASET_USERS)   || 10000;

// ── setup() 타임아웃 ──────────────────────────────────────────────
// grading/list 샘플 수집 + answer 풀 수집이 추가되므로 기존보다 여유 있게.
export const SETUP_TIMEOUT = __ENV.SETUP_TIMEOUT || '10m';

// ── 스킵 옵션 ─────────────────────────────────────────────────────
export const SKIP_SEED            = __ENV.SKIP_SEED            === 'true';
export const SKIP_WIREMOCK_CHECK  = __ENV.SKIP_WIREMOCK_CHECK  === 'true';

export const DEBUG = __ENV.DEBUG === 'true';

export const DEFAULT_HEADERS = (token) => ({
  'Content-Type': 'application/json',
  Authorization: `Bearer ${token}`,
});
