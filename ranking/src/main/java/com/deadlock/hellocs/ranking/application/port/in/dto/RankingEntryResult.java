package com.deadlock.hellocs.ranking.application.port.in.dto;

public record RankingEntryResult(
        long rank,
        Long userId,
        long score
) {}
