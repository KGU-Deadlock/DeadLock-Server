/**
 * profile.js — ops/perf/profiles/*.env 에서 run.ps1이 생성한
 * k6/.perf-profile.json 을 로드해 엔드포인트 비율·SLO·토큰풀 설정을 공급.
 *
 * run.ps1이 테스트 전에 이 파일을 생성하므로, 파일이 없으면
 * k6가 init 단계에서 오류를 냅니다.
 */

const _raw = open('../.perf-profile.json');

/** @type {{
 *   endpoints: Record<string, {ratio: number, p95: number|null}>,
 *   tokenPool: {size: number, wPower: number, wRegular: number, wCasual: number},
 *   dataset: {users: number, segPowerShare: number, segRegularShare: number, segCasualShare: number}
 * }} */
export const PROFILE = JSON.parse(_raw);

/** 엔드포인트 이름으로 RATIO_* 값을 반환. 미등록 시 0. */
export function ratioOf(name) {
  return PROFILE.endpoints[name]?.ratio ?? 0;
}

/** 엔드포인트 이름으로 P95_* 값을 반환. 빈 값이면 null (임계값 미적용). */
export function p95Of(name) {
  return PROFILE.endpoints[name]?.p95 ?? null;
}
