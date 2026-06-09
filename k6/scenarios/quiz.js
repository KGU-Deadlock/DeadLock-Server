import http from 'k6/http';
import { BASE_URL, QUIZ_THRESHOLDS, DEFAULT_HEADERS, MAX_RPS_QUIZ, MAX_VUS, SETUP_TIMEOUT } from '../lib/config.js';
import { buildTokenPool, pickToken } from '../lib/auth.js';
import { checkWiremock, seedData, verifyAiRouting } from '../lib/init.js';
import {
  validateQuizFetch,
  validateQuizGrading,
  validateQuizGradingDetail,
  validateQuizGradingDetailItem,
} from '../lib/validators.js';

const TOPIC_IDS = [1, 2, 3, 4, 5, 6];

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: {
    quiz: {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: Math.ceil(MAX_RPS_QUIZ / 2),
      maxVUs: MAX_VUS,
      stages: [
        { duration: '30s', target: 3 },
        { duration: '3m', target: MAX_RPS_QUIZ },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: QUIZ_THRESHOLDS,
};

export function setup() {
  checkWiremock();
  seedData();
  const tokens = buildTokenPool();
  verifyAiRouting(tokens[0]);
  return { tokens };
}

export default function (data) {
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

function buildAnswers(result) {
  const answers = [];

  if (result.oxQuizzes) {
    result.oxQuizzes.forEach((q) => {
      if (q.id) answers.push({ quizId: q.id, answer: 'true' });
    });
  }
  if (result.multipleChoiceQuizzes) {
    result.multipleChoiceQuizzes.forEach((q) => {
      if (q.id) answers.push({ quizId: q.id, answer: '1' });
    });
  }
  if (result.shortAnswerQuizzes) {
    result.shortAnswerQuizzes.forEach((q) => {
      if (q.id) answers.push({ quizId: q.id, answer: 'test answer' });
    });
  }

  return answers;
}
