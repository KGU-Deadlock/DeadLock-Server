import http from 'k6/http';
import { BASE_URL, DEFAULT_HEADERS, MAX_RPS_RANKING, MAX_VUS, SETUP_TIMEOUT } from '../lib/config.js';
import { buildTokenPool, pickToken } from '../lib/auth.js';
import { checkWiremock, seedData, verifyAiRouting } from '../lib/init.js';
import { validateRankingSummary, validateRankingDetail } from '../lib/validators.js';

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
        { duration: '30s', target: 5 },
        { duration: '3m', target: MAX_RPS_RANKING },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    'http_req_duration{name:ranking_summary}': ['p(95)<500'],
    'http_req_duration{name:ranking_detail}': ['p(95)<500'],
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
