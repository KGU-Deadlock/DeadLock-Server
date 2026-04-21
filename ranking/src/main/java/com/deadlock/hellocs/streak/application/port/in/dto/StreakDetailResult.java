package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record StreakDetailResult(
        @Schema(example = "4")
        int currentStreakDays,

        @Schema(example = "87")
        int solvedQuizCount,

        @Schema(example = "5")
        int solvedTopicCount,

        @Schema(example = "12")
        int longestStreakDays,

        @Schema(nullable = true, example = "2025-12-10")
        LocalDate lastSolvedDate,

        @Schema(example = "true")
        boolean solvedToday,

        @Schema(example = "10")
        int activeDaysThisMonth,

        @Schema(example = "23")
        int currentMonthSolvedQuizCount
) {}
