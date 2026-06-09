import { check } from 'k6';

// ================================================================
// 응답 검증 — 3단계 정책
// 1. 상태 코드 & envelope (status 200, isSuccess, data 존재)
// 2. 타입 & 범위 (필드 타입, 합리적 범위)
// 3. Mock/시드 고정값 (WireMock 목 또는 시드 기반 기댓값)
// ================================================================

// WireMock AI 채점 고정값 (ops/wiremock/mappings/ai-grading.json)
const AI_SCORE = 80;
const AI_MISSING_KEYWORDS_COUNT = 2;
const AI_IMPROVED_ANSWER = '더 나은 답변 예시입니다.';
const AI_FEEDBACK = '정답과 유사하지만 일부 키워드가 누락되었습니다.';

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const VALID_QUIZ_TYPES = ['OX', '단답형', '객관식', '음성'];

function parse(res) {
  try { return JSON.parse(res.body); } catch (_) { return null; }
}

export function validateRankingSummary(res, name) {
  const body = parse(res);
  const data = body?.data;
  return check(res, {
    [`${name}: status 200`]:                (r) => r.status === 200,
    [`${name}: isSuccess`]:                 () => body?.isSuccess === true,
    [`${name}: data 존재`]:                  () => data != null,
    [`${name}: topEntries 배열`]:            () => Array.isArray(data?.topEntries),
    [`${name}: totalCount >= 0`]:           () => Number.isInteger(data?.totalCount) && data.totalCount >= 0,
    [`${name}: 상위항목 rank >= 1`]:         () => !data?.topEntries?.length || data.topEntries[0].rank >= 1,
    [`${name}: 상위항목 score >= 0`]:        () => !data?.topEntries?.length || data.topEntries[0].score >= 0,
    [`${name}: 상위항목 nickname 문자열`]:   () => !data?.topEntries?.length || typeof data.topEntries[0].nickname === 'string',
    [`${name}: 상위항목 interests 배열`]:    () => !data?.topEntries?.length || Array.isArray(data.topEntries[0].interests),
  });
}

export function validateRankingDetail(res, name) {
  const body = parse(res);
  const data = body?.data;
  return check(res, {
    [`${name}: status 200`]:            (r) => r.status === 200,
    [`${name}: isSuccess`]:             () => body?.isSuccess === true,
    [`${name}: data 존재`]:              () => data != null,
    [`${name}: filterType ALL`]:        () => data?.filterType === 'ALL',
    [`${name}: rankings 배열`]:         () => Array.isArray(data?.rankings),
    [`${name}: rankings 최대 10개`]:    () => Array.isArray(data?.rankings) && data.rankings.length <= 10,
    [`${name}: nearbyRankings 배열`]:   () => Array.isArray(data?.nearbyRankings),
    [`${name}: totalCount >= 0`]:       () => Number.isInteger(data?.totalCount) && data.totalCount >= 0,
    [`${name}: myRank 객체`]:           () => data?.myRank != null && typeof data.myRank === 'object',
    [`${name}: myRank.userId 존재`]:    () => data?.myRank?.userId != null,
    [`${name}: myRank.nickname 문자열`]: () => typeof data?.myRank?.nickname === 'string',
  });
}

export function validateStreakSummary(res, name) {
  const body = parse(res);
  const data = body?.data;
  return check(res, {
    [`${name}: status 200`]:             (r) => r.status === 200,
    [`${name}: isSuccess`]:              () => body?.isSuccess === true,
    [`${name}: data 존재`]:               () => data != null,
    [`${name}: currentStreakDays >= 0`]: () => Number.isInteger(data?.currentStreakDays) && data.currentStreakDays >= 0,
    [`${name}: solvedQuizCount >= 0`]:  () => Number.isInteger(data?.solvedQuizCount) && data.solvedQuizCount >= 0,
    [`${name}: solvedTopicCount >= 0`]: () => Number.isInteger(data?.solvedTopicCount) && data.solvedTopicCount >= 0,
  });
}

export function validateStreakDetail(res, name) {
  const body = parse(res);
  const data = body?.data;
  return check(res, {
    [`${name}: status 200`]:                 (r) => r.status === 200,
    [`${name}: isSuccess`]:                  () => body?.isSuccess === true,
    [`${name}: data 존재`]:                   () => data != null,
    [`${name}: currentStreakDays >= 0`]:     () => Number.isInteger(data?.currentStreakDays) && data.currentStreakDays >= 0,
    [`${name}: solvedQuizCount >= 0`]:      () => Number.isInteger(data?.solvedQuizCount) && data.solvedQuizCount >= 0,
    [`${name}: solvedTopicCount >= 0`]:     () => Number.isInteger(data?.solvedTopicCount) && data.solvedTopicCount >= 0,
    [`${name}: longestStreakDays >= 0`]:    () => Number.isInteger(data?.longestStreakDays) && data.longestStreakDays >= 0,
    [`${name}: solvedToday boolean`]:       () => typeof data?.solvedToday === 'boolean',
    [`${name}: activeDaysThisMonth 범위`]:  () => Number.isInteger(data?.activeDaysThisMonth) && data.activeDaysThisMonth >= 0 && data.activeDaysThisMonth <= 31,
    [`${name}: lastSolvedDate 형식`]:       () => data?.lastSolvedDate == null || DATE_PATTERN.test(data.lastSolvedDate),
  });
}

export function validateStreakMonthly(res, name, year, month) {
  const body = parse(res);
  const data = body?.data;
  return check(res, {
    [`${name}: status 200`]:                   (r) => r.status === 200,
    [`${name}: isSuccess`]:                    () => body?.isSuccess === true,
    [`${name}: data 존재`]:                     () => data != null,
    [`${name}: year 일치`]:                     () => data?.year === year,
    [`${name}: month 일치`]:                    () => data?.month === month,
    [`${name}: days 배열`]:                     () => Array.isArray(data?.days),
    [`${name}: days 항목 date 형식`]:            () => !data?.days?.length || DATE_PATTERN.test(data.days[0].date),
    [`${name}: days 항목 solved boolean`]:       () => !data?.days?.length || typeof data.days[0].solved === 'boolean',
    [`${name}: days 항목 streakDay >= 0`]:       () => !data?.days?.length || data.days[0].streakDay >= 0,
  });
}

export function validateQuizFetch(res, name) {
  const body = parse(res);
  const data = body?.data;
  return check(res, {
    [`${name}: status 200`]:                    (r) => r.status === 200,
    [`${name}: isSuccess`]:                     () => body?.isSuccess === true,
    [`${name}: data 존재`]:                      () => data != null,
    [`${name}: oxQuizzes 배열`]:                () => Array.isArray(data?.oxQuizzes),
    [`${name}: multipleChoiceQuizzes 배열`]:    () => Array.isArray(data?.multipleChoiceQuizzes),
    [`${name}: shortAnswerQuizzes 배열`]:       () => Array.isArray(data?.shortAnswerQuizzes),
    [`${name}: 퀴즈 1개 이상`]: () => {
      if (!data) return false;
      return (
        (data.oxQuizzes?.length ?? 0) +
        (data.multipleChoiceQuizzes?.length ?? 0) +
        (data.shortAnswerQuizzes?.length ?? 0)
      ) > 0;
    },
    [`${name}: OX id 숫자`]:                    () => !data?.oxQuizzes?.length || typeof data.oxQuizzes[0].id === 'number',
    [`${name}: OX content 문자열`]:             () => !data?.oxQuizzes?.length || typeof data.oxQuizzes[0].content === 'string',
    [`${name}: 객관식 choices 2개 이상`]:       () =>
      !data?.multipleChoiceQuizzes?.length ||
      (Array.isArray(data.multipleChoiceQuizzes[0].choices) && data.multipleChoiceQuizzes[0].choices.length >= 2),
  });
}

export function validateQuizGrading(res, name) {
  const body = parse(res);
  const data = body?.data;
  return check(res, {
    [`${name}: status 200`]:         (r) => r.status === 200,
    [`${name}: isSuccess`]:          () => body?.isSuccess === true,
    [`${name}: gradingLogId 문자열`]: () => typeof data?.gradingLogId === 'string' && data.gradingLogId.length > 0,
  });
}

export function validateQuizGradingDetail(res, name) {
  const body = parse(res);
  const data = body?.data;
  return check(res, {
    [`${name}: status 200`]:                    (r) => r.status === 200,
    [`${name}: isSuccess`]:                     () => body?.isSuccess === true,
    [`${name}: data 존재`]:                      () => data != null,
    [`${name}: correctCount >= 0`]:             () => Number.isInteger(data?.correctCount) && data.correctCount >= 0,
    [`${name}: quizCount >= correctCount`]:     () => Number.isInteger(data?.quizCount) && data.quizCount >= (data?.correctCount ?? 0),
    [`${name}: gradingResults 배열`]:           () => Array.isArray(data?.gradingResults),
    [`${name}: 항목 quizId 숫자`]:              () => !data?.gradingResults?.length || typeof data.gradingResults[0].quizId === 'number',
    [`${name}: 항목 isCorrect boolean`]:        () => !data?.gradingResults?.length || typeof data.gradingResults[0].isCorrect === 'boolean',
    [`${name}: 항목 quizType 유효`]:            () => !data?.gradingResults?.length || VALID_QUIZ_TYPES.includes(data.gradingResults[0].quizType),
  });
}

// 단답형 퀴즈 상세 검증 — WireMock AI 채점 고정값 확인
export function validateQuizGradingDetailItem(res, name) {
  const body = parse(res);
  const data = body?.data;
  return check(res, {
    [`${name}: status 200`]:               (r) => r.status === 200,
    [`${name}: isSuccess`]:                () => body?.isSuccess === true,
    [`${name}: data 존재`]:                 () => data != null,
    [`${name}: score === ${AI_SCORE}`]:    () => data?.score === AI_SCORE,
    [`${name}: missingKeywords 길이 ${AI_MISSING_KEYWORDS_COUNT}`]: () =>
      Array.isArray(data?.missingKeywords) && data.missingKeywords.length === AI_MISSING_KEYWORDS_COUNT,
    [`${name}: improvedAnswer 일치`]:      () => data?.improvedAnswer === AI_IMPROVED_ANSWER,
    [`${name}: feedback 일치`]:            () => data?.feedback === AI_FEEDBACK,
  });
}
