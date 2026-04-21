package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DailyStreakRecordResult(
        @Schema(example = "2025-12-10")
        String date,

        @Schema(example = "true")
        boolean solved,

        @Schema(example = "3")
        int quizCount,

        @Schema(example = "4")
        int streakAtEndOfDay
) {}
