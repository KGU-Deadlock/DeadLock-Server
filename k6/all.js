/**
 * all.js — HelloCS 통합 부하테스트 엔트리.
 *
 * api.env 의 RATIO_*/P95_* 를 run.ps1 이 .perf-profile.json 으로 변환하면
 * 이 스크립트가 init 단계에서 읽어 12개 시나리오와 임계값을 동적 생성한다.
 *
 * TARGET_RPS × (RATIO / 100) = 각 엔드포인트의 목표 RPS.
 * -Module 필터로 특정 모듈만 실행해도 비율·RPS 는 그대로 유지된다(step 2 지원).
 */
import http from 'k6/http';
import {
  BASE_URL, DEFAULT_HEADERS,
  TARGET_RPS, MODULE, MAX_VUS, SETUP_TIMEOUT,
  TOKEN_POOL_SIZE, SKIP_SEED, DEBUG,
} from './lib/config.js';
import { buildTokenPool, pickToken } from './lib/auth.js';
import { checkWiremock, seedData, verifyAiRouting } from './lib/init.js';
import { PROFILE } from './lib/profile.js';
import { REGISTRY } from './lib/endpoints.js';
import {
  validateRankingSummary, validateRankingDetail,
  validateStreakSummary, validateStreakDetail, validateStreakMonthly,
  validateQuizFetch, validateQuizGrading, validateQuizGradingDetail, validateQuizGradingDetailItem,
  validateGradingList, validateUserMe, validateTopics,
} from './lib/validators.js';

const dbg = (...args) => { if (DEBUG) console.log(...args); };

// ── 활성 엔드포인트 필터링 ────────────────────────────────────────
const activeEndpoints = REGISTRY.filter((ep) => {
  if (MODULE !== 'all' && ep.module !== MODULE) return false;
  return (PROFILE.endpoints[ep.name]?.ratio ?? 0) > 0;
});

// ── options: 시나리오·임계값 동적 생성 ─────────────────────────────

function buildScenarios() {
  const scenarios = {};
  for (const ep of activeEndpoints) {
    const ratio  = PROFILE.endpoints[ep.name].ratio;
    const target = Math.max(1, Math.round(TARGET_RPS * ratio / 100));
    scenarios[ep.name.toLowerCase()] = {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: Math.ceil(target / 2),
      maxVUs: MAX_VUS,
      stages: [
        { duration: '30s', target: Math.min(5, target) }, // JIT 워밍업
        { duration: '3m',  target },                      // 목표 RPS 점진 증가
        { duration: '30s', target: 0 },
      ],
      exec: ep.exec,
    };
  }
  return scenarios;
}

function buildThresholds() {
  const thresholds = {
    // 전역 에러율 < 1% (plan SLO)
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '30s' }],
  };
  for (const ep of activeEndpoints) {
    const p95 = PROFILE.endpoints[ep.name]?.p95;
    if (!p95) continue; // P95_* 빈 값(예: GRADING_LIST) → 임계값 미적용
    thresholds[`http_req_duration{name:${ep.tag}}`] = [
      { threshold: `p(95)<${p95}`, abortOnFail: true, delayAbortEval: '30s' },
    ];
  }
  return thresholds;
}

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: buildScenarios(),
  thresholds: buildThresholds(),
};

// ── setup ────────────────────────────────────────────────────────

export function setup() {
  checkWiremock();
  if (!SKIP_SEED) seedData();

  // 1. 토큰 풀 생성
  const poolSize = PROFILE.tokenPool?.size ?? TOKEN_POOL_SIZE;
  const tokens   = buildTokenPool(poolSize);
  dbg(`[setup] 토큰 풀 ${tokens.length}개 생성`);

  verifyAiRouting(tokens[0]);

  const tokenConfig = PROFILE.tokenPool
    ? {
        wPower:         PROFILE.tokenPool.wPower,
        wRegular:       PROFILE.tokenPool.wRegular,
        wCasual:        PROFILE.tokenPool.wCasual,
        segPowerShare:  PROFILE.dataset?.segPowerShare  ?? 0.2,
        segRegularShare: PROFILE.dataset?.segRegularShare ?? 0.5,
      }
    : null;

  // 2. grading 풀 수집 (GRADING_RESULT / GRADING_DETAIL 용)
  //    사전 생성된 bake 데이터에서 gradingLogId + quizId 를 수집.
  const gradingPool = [];         // [{ token, logId, quizId }] quizId=null 이면 RESULT 전용
  const GRADING_POOL_SAMPLES = Math.min(200, tokens.length);

  if (MODULE === 'all' || MODULE === 'grading') {
    dbg(`[setup] grading 풀 수집 시작 (샘플 ${GRADING_POOL_SAMPLES}명)`);
    for (let i = 0; i < GRADING_POOL_SAMPLES; i++) {
      const token = tokens[i % tokens.length];
      const headers = DEFAULT_HEADERS(token);

      // gradingLogId 목록 수집
      const listRes = http.get(`${BASE_URL}/v1/quiz/grading/list`, { headers });
      if (listRes.status !== 200) continue;

      let logs;
      try { logs = JSON.parse(listRes.body).data; } catch (_) { continue; }
      if (!Array.isArray(logs) || logs.length === 0) continue;

      // 첫 번째 로그로 quizId 확보 (단답형 우선)
      const firstLogId = logs[0].id;
      let quizId = null;

      const detailRes = http.get(`${BASE_URL}/v1/quiz/grading/${firstLogId}`, { headers });
      if (detailRes.status === 200) {
        try {
          const results = JSON.parse(detailRes.body).data?.gradingResults;
          const shortItem = results?.find((r) => r.quizType === '단답형');
          quizId = shortItem?.quizId ?? results?.[0]?.quizId ?? null;
        } catch (_) { /* skip */ }
      }

      // 모든 logId 를 풀에 추가 (quizId 는 첫 번째에만 붙임)
      logs.forEach((log, idx) => {
        gradingPool.push({ token, logId: log.id, quizId: idx === 0 ? quizId : null });
      });
    }
    dbg(`[setup] grading 풀 ${gradingPool.length}개 항목 수집`);
  }

  // 3. answer 풀 수집 (GRADING_SUBMIT 용)
  //    quiz bank 에서 유효한 quizId 를 확보한다.
  //    동일 answer 세트를 여러 VU 가 재사용해도 grading 서비스는 새 로그를 생성함.
  const answerPool = [];
  const ANSWER_POOL_SAMPLES = Math.min(100, tokens.length);

  if (MODULE === 'all' || MODULE === 'grading' || MODULE === 'quiz') {
    dbg(`[setup] answer 풀 수집 시작 (샘플 ${ANSWER_POOL_SAMPLES}명)`);
    const TOPIC_IDS = [1, 2, 3, 4, 5, 6];
    for (let i = 0; i < ANSWER_POOL_SAMPLES; i++) {
      const token    = tokens[i % tokens.length];
      const headers  = DEFAULT_HEADERS(token);
      const topicId  = TOPIC_IDS[i % TOPIC_IDS.length];

      const fetchRes = http.post(
        `${BASE_URL}/v1/quiz`,
        JSON.stringify({ topicIds: [topicId], mode: 'STANDARD' }),
        { headers }
      );
      if (fetchRes.status !== 200) continue;

      try {
        const result = JSON.parse(fetchRes.body).data;
        const answers = buildAnswers(result);
        if (answers.length > 0) answerPool.push({ answers });
      } catch (_) { /* skip */ }
    }
    dbg(`[setup] answer 풀 ${answerPool.length}개 세트 수집`);
  }

  if ((MODULE === 'all' || MODULE === 'grading') && gradingPool.length === 0) {
    console.warn('[setup] grading 풀이 비어있습니다. bake 데이터셋이 있는지 확인하세요.');
  }
  if ((MODULE === 'all' || MODULE === 'grading' || MODULE === 'quiz') && answerPool.length === 0) {
    console.warn('[setup] answer 풀이 비어있습니다. 퀴즈 뱅크 시드를 확인하세요.');
  }

  return { tokens, tokenConfig, gradingPool, answerPool };
}

// ── 헬퍼 ─────────────────────────────────────────────────────────

function buildAnswers(result) {
  const answers = [];
  (result?.oxQuizzes            ?? []).forEach((q) => { if (q.id) answers.push({ quizId: q.id, answer: 'true' }); });
  (result?.multipleChoiceQuizzes ?? []).forEach((q) => { if (q.id) answers.push({ quizId: q.id, answer: '1' }); });
  (result?.shortAnswerQuizzes   ?? []).forEach((q) => { if (q.id) answers.push({ quizId: q.id, answer: 'test answer' }); });
  return answers;
}

function randomFrom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

// ── 핸들러 (k6 exec 로 참조) ─────────────────────────────────────
// 모두 export 필수: k6가 시나리오 exec 이름을 이 파일의 export 에서 찾음.

export function execStreakSummary(data) {
  const token = pickToken(data.tokens, data.tokenConfig);
  const res = http.get(`${BASE_URL}/v1/streak`, {
    headers: DEFAULT_HEADERS(token),
    tags: { name: 'streak_summary' },
  });
  validateStreakSummary(res, 'streak_summary');
}

export function execStreakMonthly(data) {
  const token = pickToken(data.tokens, data.tokenConfig);
  const now   = new Date();
  const res   = http.get(
    `${BASE_URL}/v1/streak?year=${now.getFullYear()}&month=${now.getMonth() + 1}`,
    { headers: DEFAULT_HEADERS(token), tags: { name: 'streak_monthly' } }
  );
  validateStreakMonthly(res, 'streak_monthly', now.getFullYear(), now.getMonth() + 1);
}

export function execStreakDetail(data) {
  const token = pickToken(data.tokens, data.tokenConfig);
  const res = http.get(`${BASE_URL}/v1/streak/detail`, {
    headers: DEFAULT_HEADERS(token),
    tags: { name: 'streak_detail' },
  });
  validateStreakDetail(res, 'streak_detail');
}

export function execRankingSummary(data) {
  const token = pickToken(data.tokens, data.tokenConfig);
  const res = http.get(`${BASE_URL}/v1/ranking/summary`, {
    headers: DEFAULT_HEADERS(token),
    tags: { name: 'ranking_summary' },
  });
  validateRankingSummary(res, 'ranking_summary');
}

export function execRankingPage(data) {
  const token = pickToken(data.tokens, data.tokenConfig);
  const res = http.get(`${BASE_URL}/v1/ranking?filterType=ALL&size=10`, {
    headers: DEFAULT_HEADERS(token),
    tags: { name: 'ranking_page' },
  });
  validateRankingDetail(res, 'ranking_page');
}

export function execQuizFetch(data) {
  const token   = pickToken(data.tokens, data.tokenConfig);
  const topicId = ((__VU - 1) % 6) + 1;
  const res = http.post(
    `${BASE_URL}/v1/quiz`,
    JSON.stringify({ topicIds: [topicId], mode: 'STANDARD' }),
    { headers: DEFAULT_HEADERS(token), tags: { name: 'quiz_fetch' } }
  );
  validateQuizFetch(res, 'quiz_fetch');
}

export function execGradingSubmit(data) {
  if (data.answerPool.length === 0) return;
  const token = pickToken(data.tokens, data.tokenConfig);
  const { answers } = randomFrom(data.answerPool);
  const res = http.post(
    `${BASE_URL}/v1/quiz/grading`,
    JSON.stringify(answers),
    { headers: DEFAULT_HEADERS(token), tags: { name: 'grading_submit' } }
  );
  validateQuizGrading(res, 'grading_submit');
}

export function execGradingResult(data) {
  if (data.gradingPool.length === 0) return;
  const entry = randomFrom(data.gradingPool);
  const res = http.get(
    `${BASE_URL}/v1/quiz/grading/${entry.logId}`,
    { headers: DEFAULT_HEADERS(entry.token), tags: { name: 'grading_result' } }
  );
  validateQuizGradingDetail(res, 'grading_result');
}

export function execGradingDetail(data) {
  const detailPool = data.gradingPool.filter((e) => e.quizId != null);
  if (detailPool.length === 0) return;
  const entry = randomFrom(detailPool);
  const res = http.get(
    `${BASE_URL}/v1/quiz/grading/${entry.logId}/${entry.quizId}`,
    { headers: DEFAULT_HEADERS(entry.token), tags: { name: 'grading_detail' } }
  );
  validateQuizGradingDetailItem(res, 'grading_detail');
}

export function execGradingList(data) {
  const token = pickToken(data.tokens, data.tokenConfig);
  const res = http.get(`${BASE_URL}/v1/quiz/grading/list`, {
    headers: DEFAULT_HEADERS(token),
    tags: { name: 'grading_list' },
  });
  validateGradingList(res, 'grading_list');
}

export function execUserMe(data) {
  const token = pickToken(data.tokens, data.tokenConfig);
  const res = http.get(`${BASE_URL}/v1/users/me`, {
    headers: DEFAULT_HEADERS(token),
    tags: { name: 'user_me' },
  });
  validateUserMe(res, 'user_me');
}

export function execTopics(data) {
  const token = pickToken(data.tokens, data.tokenConfig);
  const res = http.get(`${BASE_URL}/v1/topics`, {
    headers: DEFAULT_HEADERS(token),
    tags: { name: 'topics' },
  });
  validateTopics(res, 'topics');
}

// ── 결과 저장 ─────────────────────────────────────────────────────

export function handleSummary(data) {
  const file = __ENV.RESULTS_FILE || 'k6/results/latest-summary.json';
  return { [file]: JSON.stringify(data, null, 2) };
}
