package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

public record UpdateRankingCommand(
        Long userId,
        List<Long> topicIds,
        int score
) {}
