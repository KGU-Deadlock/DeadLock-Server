import http from 'k6/http';
import { BASE_URL, SEED_USER_COUNT } from './config.js';

const KAKAO_ID_BASE = 1001;

export function buildTokenPool() {
  const tokens = [];
  for (let i = 0; i < SEED_USER_COUNT; i++) {
    const kakaoId = KAKAO_ID_BASE + i;
    const res = http.get(`${BASE_URL}/v1/dev/user-token?kakaoId=${kakaoId}`);
    if (res.status === 200) {
      const body = JSON.parse(res.body);
      if (body.data && body.data.accessToken) {
        tokens.push(body.data.accessToken);
      }
    }
  }
  if (tokens.length === 0) {
    throw new Error('토큰 풀 생성 실패. setup()에서 seedData()가 먼저 실행됐는지 확인하세요.');
  }
  return tokens;
}

export function pickToken(tokens) {
  return tokens[(__VU - 1) % tokens.length];
}
