package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

public record MyRankingResult(
        Long userId,
        String nickname,
        String profileImage,
        List<String> interests,
        Long rank,
        long score
) {}
