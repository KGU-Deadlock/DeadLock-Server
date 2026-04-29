package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 달력의 하루 단위 스트릭 기록 응답 DTO.
 *
 * @param date             날짜 (yyyy-MM-dd 형식)
 * @param solved           당일 퀴즈 풀이 여부
 * @param quizCount        당일 풀이한 퀴즈 수
 * @param streakAtEndOfDay 당일 기준 연속 스트릭 일수
 */
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
