package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record StreakMonthlyResult(
        @Schema(example = "2025")
        int year,

        @Schema(example = "12")
        int month,

        List<DailyStreakRecordResult> days
) {}
