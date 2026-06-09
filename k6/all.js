import http from 'k6/http';
import { BASE_URL, DEFAULT_HEADERS, MAX_RPS_RANKING, MAX_RPS_STREAK, MAX_RPS_QUIZ, MAX_VUS, SETUP_TIMEOUT } from './lib/config.js';
import { buildTokenPool, pickToken } from './lib/auth.js';
import { checkWiremock, seedData, verifyAiRouting } from './lib/init.js';
import {
  validateRankingSummary, validateRankingDetail,
  validateStreakSummary, validateStreakDetail, validateStreakMonthly,
  validateQuizFetch, validateQuizGrading, validateQuizGradingDetail, validateQuizGradingDetailItem,
} from './lib/validators.js';

const TOPIC_IDS = [1, 2, 3, 4, 5, 6];

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: {
    ranking: {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: Math.ceil(MAX_RPS_RANKING / 2),
      maxVUs: MAX_VUS,
      stages: [
        { duration: '30s', target: 5 },              // JIT 워밍업
        { duration: '3m', target: MAX_RPS_RANKING }, // 한계까지 점진 증가
        { duration: '30s', target: 0 },
      ],
      exec: 'rankingScenario',
    },
    streak: {
      executor: 'ramping-arrival-rate',
      startTime: '30s',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: Math.ceil(MAX_RPS_STREAK / 2),
      maxVUs: MAX_VUS,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '3m', target: MAX_RPS_STREAK },
        { duration: '30s', target: 0 },
      ],
      exec: 'streakScenario',
    },
    quiz: {
      executor: 'ramping-arrival-rate',
      startTime: '1m',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: Math.ceil(MAX_RPS_QUIZ / 2),
      maxVUs: MAX_VUS,
      stages: [
        { duration: '30s', target: 3 },
        { duration: '3m', target: MAX_RPS_QUIZ },
        { duration: '30s', target: 0 },
      ],
      exec: 'quizScenario',
    },
  },
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: true }],
    'http_req_duration{name:ranking_summary}': [{ threshold: 'p(95)<500',  abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{name:ranking_detail}':  [{ threshold: 'p(95)<500',  abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{name:streak_summary}':  [{ threshold: 'p(95)<700',  abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{name:streak_detail}':   [{ threshold: 'p(95)<700',  abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{name:streak_monthly}':          [{ threshold: 'p(95)<700',  abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{name:quiz_fetch}':              [{ threshold: 'p(95)<1000', abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{name:quiz_grading}':            [{ threshold: 'p(95)<3000', abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{name:quiz_grading_detail}':     [{ threshold: 'p(95)<1000', abortOnFail: true, delayAbortEval: '30s' }],
    'http_req_duration{name:quiz_grading_detail_item}': [{ threshold: 'p(95)<1000', abortOnFail: true, delayAbortEval: '30s' }],
  },
};

export function setup() {
  checkWiremock();
  seedData();
  const tokens = buildTokenPool();
  verifyAiRouting(tokens[0]);
  return { tokens };
}

export function rankingScenario(data) {
  const token = pickToken(data.tokens);
  const headers = DEFAULT_HEADERS(token);

  const summaryRes = http.get(`${BASE_URL}/v1/ranking/summary`, {
    tags: { name: 'ranking_summary' },
  });
  validateRankingSummary(summaryRes, 'ranking_summary');

  const detailRes = http.get(`${BASE_URL}/v1/ranking?filterType=ALL&size=10`, {
    headers,
    tags: { name: 'ranking_detail' },
  });
  validateRankingDetail(detailRes, 'ranking_detail');
}

export function streakScenario(data) {
  const token = pickToken(data.tokens);
  const headers = DEFAULT_HEADERS(token);

  const summaryRes = http.get(`${BASE_URL}/v1/streak`, {
    headers,
    tags: { name: 'streak_summary' },
  });
  validateStreakSummary(summaryRes, 'streak_summary');

  const detailRes = http.get(`${BASE_URL}/v1/streak/detail`, {
    headers,
    tags: { name: 'streak_detail' },
  });
  validateStreakDetail(detailRes, 'streak_detail');

  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;
  const monthlyRes = http.get(
    `${BASE_URL}/v1/streak?year=${year}&month=${month}`,
    { headers, tags: { name: 'streak_monthly' } }
  );
  validateStreakMonthly(monthlyRes, 'streak_monthly', year, month);
}

export function quizScenario(data) {
  const token = pickToken(data.tokens);
  const headers = DEFAULT_HEADERS(token);
  const topicId = TOPIC_IDS[(__VU - 1) % TOPIC_IDS.length];

  const fetchRes = http.post(
    `${BASE_URL}/v1/quiz`,
    JSON.stringify({ topicIds: [topicId], mode: 'STANDARD' }),
    { headers, tags: { name: 'quiz_fetch' } }
  );
  validateQuizFetch(fetchRes, 'quiz_fetch');

  if (fetchRes.status !== 200) return;

  let answers;
  try {
    const result = JSON.parse(fetchRes.body).data;
    answers = buildAnswers(result);
  } catch (_) {
    return;
  }

  if (answers.length === 0) return;

  const gradingRes = http.post(
    `${BASE_URL}/v1/quiz/grading`,
    JSON.stringify(answers),
    { headers, tags: { name: 'quiz_grading' } }
  );
  validateQuizGrading(gradingRes, 'quiz_grading');

  if (gradingRes.status !== 200) return;

  let gradingLogId;
  try {
    gradingLogId = JSON.parse(gradingRes.body).data?.gradingLogId;
  } catch (_) {
    return;
  }

  if (!gradingLogId) return;

  const gradingDetailRes = http.get(
    `${BASE_URL}/v1/quiz/grading/${gradingLogId}`,
    { headers, tags: { name: 'quiz_grading_detail' } }
  );
  validateQuizGradingDetail(gradingDetailRes, 'quiz_grading_detail');

  if (gradingDetailRes.status !== 200) return;

  let shortAnswerItem;
  try {
    const gradingResults = JSON.parse(gradingDetailRes.body).data?.gradingResults;
    shortAnswerItem = gradingResults?.find((r) => r.quizType === '단답형');
  } catch (_) {
    return;
  }

  if (!shortAnswerItem) return;

  const gradingDetailItemRes = http.get(
    `${BASE_URL}/v1/quiz/grading/${gradingLogId}/${shortAnswerItem.quizId}`,
    { headers, tags: { name: 'quiz_grading_detail_item' } }
  );
  validateQuizGradingDetailItem(gradingDetailItemRes, 'quiz_grading_detail_item');
}

export function handleSummary(data) {
  const file = __ENV.RESULTS_FILE || 'k6/results/latest-summary.json';
  return { [file]: JSON.stringify(data, null, 2) };
}

function buildAnswers(result) {
  const answers = [];
  if (result.oxQuizzes) {
    result.oxQuizzes.forEach((q) => { if (q.id) answers.push({ quizId: q.id, answer: 'true' }); });
  }
  if (result.multipleChoiceQuizzes) {
    result.multipleChoiceQuizzes.forEach((q) => { if (q.id) answers.push({ quizId: q.id, answer: '1' }); });
  }
  if (result.shortAnswerQuizzes) {
    result.shortAnswerQuizzes.forEach((q) => { if (q.id) answers.push({ quizId: q.id, answer: 'test answer' }); });
  }
  return answers;
}
