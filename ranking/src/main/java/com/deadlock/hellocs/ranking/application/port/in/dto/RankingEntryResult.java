package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

public record RankingEntryResult(
        long rank,
        Long userId,
        String nickname,
        String profileImage,
        List<String> interests,
        long score
) {}
