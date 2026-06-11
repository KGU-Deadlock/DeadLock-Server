import http from 'k6/http';
import { BASE_URL, TOKEN_POOL_SIZE } from './config.js';

// bake 컨벤션: kakaoId 는 1001 부터 순차 시드됨.
const KAKAO_ID_BASE = 1001;

/**
 * 토큰 풀 빌드.
 * size 개의 사용자(kakaoId 1001..1001+size-1)에 대한 JWT 를 수집한다.
 *
 * @param {number} [size] - 수집할 토큰 수. 미지정 시 TOKEN_POOL_SIZE 환경변수 사용.
 * @returns {string[]}
 */
export function buildTokenPool(size) {
  const poolSize = size || TOKEN_POOL_SIZE;

  const requests = [];
  for (let i = 0; i < poolSize; i++) {
    requests.push(['GET', `${BASE_URL}/v1/dev/user-token?kakaoId=${KAKAO_ID_BASE + i}`]);
  }

  const responses = http.batch(requests);
  const tokens = [];
  for (const res of responses) {
    if (res.status === 200) {
      try {
        const body = JSON.parse(res.body);
        if (body.data?.accessToken) tokens.push(body.data.accessToken);
      } catch (_) {}
    }
  }

  if (tokens.length === 0) {
    throw new Error('[auth] 토큰 풀 생성 실패. dev-service 가 기동됐는지, 시드 데이터가 있는지 확인하세요.');
  }
  return tokens;
}

/**
 * 토큰 선택.
 *
 * tokenConfig 가 있으면 dataset.env 세그먼트 가중치 기반 랜덤 선택.
 * 없으면 VU 번호 기반 라운드로빈(fallback).
 *
 * 세그먼트 블록 컨벤션(bake 세션과의 정합 좌표점):
 *   토큰 풀 내 순서를 [power | regular | casual] 블록으로 간주.
 *   bake 가 kakaoId 를 동일 순서(power → regular → casual)로 시드해야
 *   가중치 효과가 현실적으로 작동함.
 *   매핑이 불명확하면 아래 segPowerShare/segRegularShare 기본값이 균등에 가까워짐.
 *
 * @param {string[]} tokens
 * @param {{ wPower: number, wRegular: number, wCasual: number,
 *            segPowerShare: number, segRegularShare: number }} [tokenConfig]
 * @returns {string}
 */
export function pickToken(tokens, tokenConfig) {
  if (!tokenConfig) {
    return tokens[(__VU - 1) % tokens.length];
  }

  const n = tokens.length;
  const { wPower = 0.3, wRegular = 0.5, segPowerShare = 0.2, segRegularShare = 0.5 } = tokenConfig;

  const powerCount   = Math.max(1, Math.floor(n * segPowerShare));
  const regularCount = Math.max(1, Math.floor(n * segRegularShare));
  const casualStart  = powerCount + regularCount;

  const r = Math.random();
  let base, count;
  if (r < wPower) {
    base = 0; count = powerCount;
  } else if (r < wPower + wRegular) {
    base = powerCount; count = regularCount;
  } else {
    base = casualStart; count = Math.max(1, n - casualStart);
  }

  return tokens[base + Math.floor(Math.random() * count)];
}
