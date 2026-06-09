import http from 'k6/http';
import {
  BASE_URL,
  WIREMOCK_URL,
  SEED_USER_COUNT,
  SEED_DAYS,
  SEED_QUIZ_PER_COMBO,
  SKIP_SEED,
  SKIP_WIREMOCK_CHECK,
  DEBUG,
} from './config.js';

const dbg = (...args) => { if (DEBUG) dbg(...args); };

/**
 * WireMock이 정상 동작 중인지 확인.
 * 응답 없으면 즉시 에러로 테스트 중단.
 */
export function checkWiremock() {
  if (SKIP_WIREMOCK_CHECK) {
    dbg('[init] WireMock 체크 스킵 (SKIP_WIREMOCK_CHECK=true)');
    return;
  }
  const res = http.get(`${WIREMOCK_URL}/__admin/health`);
  if (res.status !== 200) {
    throw new Error(
      `[init] WireMock 미응답 (${WIREMOCK_URL}). ` +
      '먼저 실행: docker compose -f docker-compose-local-infra.yaml up -d'
    );
  }
  dbg(`[init] WireMock 정상 확인 (${WIREMOCK_URL})`);
}

/**
 * 앱이 실제로 WireMock으로 AI 요청을 보내는지 검증.
 * 단답형 퀴즈를 하나 채점하고 WireMock 저널에 기록이 있는지 확인한다.
 *
 * 이 검증에 실패하면 AI_GRADING_EVALUATE_ENDPOINT가 실제 AI 서버를
 * 가리키고 있을 가능성이 있으므로 테스트를 중단한다.
 */
export function verifyAiRouting(token) {
  if (SKIP_WIREMOCK_CHECK) return;

  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

  // WireMock 저널 초기화
  http.del(`${WIREMOCK_URL}/__admin/requests`);

  // 퀴즈 조회 (단답형 포함 여부 확인)
  const fetchRes = http.post(
    `${BASE_URL}/v1/quiz`,
    JSON.stringify({ topicIds: [1], mode: 'STANDARD' }),
    { headers }
  );

  dbg(`[init/verify] 퀴즈 조회 응답: HTTP ${fetchRes.status} → ${fetchRes.body}`);
  if (fetchRes.status !== 200) {
    console.warn('[init] AI 라우팅 검증 건너뜀 — 퀴즈 조회 실패 (시드 데이터 확인 필요)');
    return;
  }

  const data = JSON.parse(fetchRes.body).data;
  if (!data || !data.shortAnswerQuizzes || data.shortAnswerQuizzes.length === 0) {
    console.warn('[init] AI 라우팅 검증 건너뜀 — 단답형 퀴즈가 응답에 없음');
    return;
  }

  // 세션에 포함된 전체 퀴즈를 한 번에 채점해야 QUIZ4002 방지
  const answers = [];
  (data.oxQuizzes || []).forEach((q) => answers.push({ quizId: q.id, answer: 'true' }));
  (data.multipleChoiceQuizzes || []).forEach((q) => answers.push({ quizId: q.id, answer: '1' }));
  (data.shortAnswerQuizzes || []).forEach((q) => answers.push({ quizId: q.id, answer: 'verify' }));

  dbg(`[init/verify] 채점 요청 — 총 ${answers.length}개 (단답형 ${data.shortAnswerQuizzes.length}개 포함)`);
  const gradingRes = http.post(
    `${BASE_URL}/v1/quiz/grading`,
    JSON.stringify(answers),
    { headers }
  );
  dbg(`[init/verify] 채점 응답: HTTP ${gradingRes.status} → ${gradingRes.body}`);

  // WireMock 저널에 /grading/evaluate 요청이 기록됐는지 확인
  const journalRes = http.get(`${WIREMOCK_URL}/__admin/requests`);
  const journal = JSON.parse(journalRes.body);

  const aiCallRouted = journal.requests && journal.requests.some(
    (r) => r.request.url.includes('/api/feedback/evaluate')
  );

  if (!aiCallRouted) {
    const recordedUrls = journal.requests
      ? journal.requests.map((r) => r.request.url).join(', ')
      : '(저널 비어있음)';
    console.error(`[init] WireMock 저널에 기록된 요청: [${recordedUrls}]`);
    throw new Error(
      '[init] AI 라우팅 검증 실패! ' +
      '앱이 WireMock 대신 실제 AI 서버로 요청을 보내고 있을 수 있습니다. ' +
      `.env.local 에서 AI_GRADING_EVALUATE_ENDPOINT=${WIREMOCK_URL}/api/feedback/evaluate 확인하세요.`
    );
  }

  dbg('[init] AI 라우팅 정상 — WireMock으로 요청 확인됨');
}

/**
 * 시드 데이터 자동 초기화.
 * 각 시드 API는 멱등(이미 있으면 skip)이므로 매 실행마다 호출해도 안전.
 */
export function seedData() {
  if (SKIP_SEED) {
    dbg('[init] 시드 스킵 (SKIP_SEED=true)');
    return;
  }

  dbg(`[init] 시드 시작 — 유저 ${SEED_USER_COUNT}명 / ${SEED_DAYS}일치 통계`);

  let res;

  res = http.post(`${BASE_URL}/v1/dev/seed/topics`);
  dbg(`[seed/topics] HTTP ${res.status} → ${res.body}`);
  if (res.status !== 200) {
    throw new Error(`[init] topics 시드 실패`);
  }

  res = http.post(`${BASE_URL}/v1/dev/seed/quiz-bank?perCombo=${SEED_QUIZ_PER_COMBO}`);
  dbg(`[seed/quiz-bank] HTTP ${res.status} → ${res.body}`);
  if (res.status !== 200) {
    throw new Error(`[init] quiz-bank 시드 실패`);
  }

  res = http.post(`${BASE_URL}/v1/dev/seed/cs-questions?perCategory=10`);
  dbg(`[seed/cs-questions] HTTP ${res.status} → ${res.body}`);
  if (res.status !== 200) {
    throw new Error(`[init] cs-questions 시드 실패`);
  }

  dbg(`[seed/stats] 시작 — 오래 걸릴 수 있습니다 (유저 ${SEED_USER_COUNT}명 × ${SEED_DAYS}일)`);
  res = http.post(
    `${BASE_URL}/v1/dev/seed/stats?userCount=${SEED_USER_COUNT}&days=${SEED_DAYS}`,
    null,
    { timeout: '600s' }
  );
  dbg(`[seed/stats] HTTP ${res.status} → ${res.body}`);
  if (res.status !== 200) {
    throw new Error(`[init] stats 시드 실패`);
  }

  dbg('[init] 시드 완료');
}
