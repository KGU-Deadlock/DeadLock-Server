/**
 * endpoints.js — 12개 엔드포인트 정적 레지스트리.
 *
 * 각 항목:
 *   name   — api.env의 RATIO_*/P95_* 키 (대문자, 언더스코어 구분)
 *   module — 모듈 필터(-Module 파라미터)에 사용
 *   tag    — k6 메트릭 태그 (http_req_duration{name:...})
 *   exec   — all.js에서 export 하는 핸들러 함수명 (k6 scenarios.exec 값)
 *
 * 요청 로직(HTTP call, 검증)은 all.js의 각 핸들러에 있습니다.
 * 비율/SLO 숫자는 profile.js (→ .perf-profile.json)에서 옵니다.
 */

export const REGISTRY = [
  // ── streak-service ─────────────────────────────────────────────
  { name: 'STREAK_SUMMARY',  module: 'streak',  tag: 'streak_summary',  exec: 'execStreakSummary'  },
  { name: 'STREAK_MONTHLY',  module: 'streak',  tag: 'streak_monthly',  exec: 'execStreakMonthly'  },
  { name: 'STREAK_DETAIL',   module: 'streak',  tag: 'streak_detail',   exec: 'execStreakDetail'   },
  // ── ranking-service ────────────────────────────────────────────
  { name: 'RANKING_SUMMARY', module: 'ranking', tag: 'ranking_summary', exec: 'execRankingSummary' },
  { name: 'RANKING_PAGE',    module: 'ranking', tag: 'ranking_page',    exec: 'execRankingPage'    },
  // ── quiz-service ────────────────────────────────────────────────
  { name: 'QUIZ_FETCH',      module: 'quiz',    tag: 'quiz_fetch',      exec: 'execQuizFetch'      },
  // ── grading-service ─────────────────────────────────────────────
  { name: 'GRADING_SUBMIT',  module: 'grading', tag: 'grading_submit',  exec: 'execGradingSubmit'  },
  { name: 'GRADING_RESULT',  module: 'grading', tag: 'grading_result',  exec: 'execGradingResult'  },
  { name: 'GRADING_DETAIL',  module: 'grading', tag: 'grading_detail',  exec: 'execGradingDetail'  },
  { name: 'GRADING_LIST',    module: 'grading', tag: 'grading_list',    exec: 'execGradingList'    },
  // ── user-service ────────────────────────────────────────────────
  { name: 'USER_ME',         module: 'user',    tag: 'user_me',         exec: 'execUserMe'         },
  // ── topic-service ───────────────────────────────────────────────
  { name: 'TOPICS',          module: 'topic',   tag: 'topics',          exec: 'execTopics'         },
];

/** -Module 파라미터로 허용되는 값 목록 */
export const VALID_MODULES = ['all', 'streak', 'ranking', 'quiz', 'grading', 'user', 'topic'];
