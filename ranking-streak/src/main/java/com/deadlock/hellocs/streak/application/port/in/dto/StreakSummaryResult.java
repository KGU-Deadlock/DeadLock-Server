package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스트릭 요약 정보 응답 DTO.
 *
 * @param currentStreakDays 현재 연속 학습 일수
 * @param solvedQuizCount   총 누적 풀이 수
 * @param solvedTopicCount  풀이한 주제(분야) 수
 */
public record StreakSummaryResult(
        @Schema(example = "4")
        int currentStreakDays,

        @Schema(example = "87")
        int solvedQuizCount,

        @Schema(example = "5")
        int solvedTopicCount
) {}
