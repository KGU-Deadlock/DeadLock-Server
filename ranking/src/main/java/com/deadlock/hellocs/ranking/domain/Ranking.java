package com.deadlock.hellocs.ranking.domain;

public record Ranking(
        Long userId,
        long score,
        long rank
) {}
