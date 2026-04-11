package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record StreakDetailResult(
        // 수정 표시
        @Schema(description = "현재 연속 학습 일수입니다.", example = "4")
        int currentStreakDays,
        // 수정 표시
        @Schema(description = "누적 퀴즈 풀이 수입니다.", example = "87")
        int solvedQuizCount,
        // 수정 표시
        @Schema(description = "해결한 분야 수입니다.", example = "5")
        int solvedTopicCount,
        // 수정 표시
        @Schema(description = "최장 연속 학습 일수입니다.", example = "12")
        int longestStreakDays,
        // 수정 표시
        @Schema(description = "마지막으로 퀴즈를 푼 날짜입니다.", example = "2025-12-10", nullable = true)
        LocalDate lastSolvedDate,
        // 수정 표시
        @Schema(description = "오늘 퀴즈를 풀었는지 여부입니다.", example = "true")
        boolean solvedToday,
        // 수정 표시
        @Schema(description = "이번 달 학습한 일수입니다.", example = "10")
        int activeDaysThisMonth,
        // 수정 표시
        @Schema(description = "이번 달 푼 퀴즈 수입니다.", example = "23")
        int currentMonthSolvedQuizCount
) {
}
