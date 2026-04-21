package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StreakSummaryResult(
        @Schema(example = "4")
        int currentStreakDays,

        @Schema(example = "87")
        int solvedQuizCount,

        @Schema(example = "5")
        int solvedTopicCount
) {}
