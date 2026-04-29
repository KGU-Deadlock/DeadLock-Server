package com.deadlock.hellocs.ranking.domain;

/**
 * 랭킹 한 건을 표현하는 도메인 모델.
 *
 * <p>Redis ZSet에서 조회한 결과를 애플리케이션 계층에서 다룰 수 있도록
 * 사용자 ID, 누적 점수, 현재 순위로 구성된 불변 값 객체로 변환함.</p>
 *
 * @param userId 해당 랭킹의 소유자 사용자 ID
 * @param score  누적 점수 (Redis ZSet score)
 * @param rank   1부터 시작하는 순위 (Redis의 0-based rank를 보정한 값)
 */
public record Ranking(
        Long userId,
        long score,
        long rank
) {}
