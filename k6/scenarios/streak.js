import http from 'k6/http';
import { BASE_URL, DEFAULT_HEADERS, MAX_RPS_STREAK, MAX_VUS, SETUP_TIMEOUT } from '../lib/config.js';
import { buildTokenPool, pickToken } from '../lib/auth.js';
import { checkWiremock, seedData, verifyAiRouting } from '../lib/init.js';
import { validateStreakSummary, validateStreakDetail, validateStreakMonthly } from '../lib/validators.js';

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: {
    streak: {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: Math.ceil(MAX_RPS_STREAK / 2),
      maxVUs: MAX_VUS,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '3m', target: MAX_RPS_STREAK },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    'http_req_duration{name:streak_summary}': ['p(95)<700'],
    'http_req_duration{name:streak_detail}': ['p(95)<700'],
    'http_req_duration{name:streak_monthly}': ['p(95)<700'],
  },
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
    {
      headers,
      tags: { name: 'streak_monthly' },
    }
  );
  validateStreakMonthly(monthlyRes, 'streak_monthly', year, month);
}
