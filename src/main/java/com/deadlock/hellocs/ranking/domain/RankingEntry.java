package com.deadlock.hellocs.ranking.domain;

public record RankingEntry(
        Long kakaoId,
        long score,
        long rank
) {
}
