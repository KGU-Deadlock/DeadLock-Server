package com.deadlock.hellocs.ranking.application.port.in.dto;

/**
 * 응답용 랭킹 한 줄(Row) DTO.
 *
 * @param rank   1부터 시작하는 순위
 * @param userId 사용자 ID
 * @param score  누적 점수
 */
public record RankingEntryResult(
        long rank,
        Long userId,
        long score
) {}
