package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 스트릭 상세 정보 응답 DTO.
 * 요약 정보에 최장 스트릭·마지막 풀이일·오늘 풀이 여부·이번 달 통계를 추가한 확장 응답임.
 *
 * @param currentStreakDays           현재 연속 학습 일수
 * @param solvedQuizCount             총 누적 풀이 수
 * @param solvedTopicCount            풀이한 주제(분야) 수
 * @param longestStreakDays            역대 최장 연속 학습 일수
 * @param lastSolvedDate              마지막 풀이 날짜 (null 가능)
 * @param solvedToday                 오늘 풀이 여부
 * @param activeDaysThisMonth         이번 달 활동(풀이) 일수
 * @param currentMonthSolvedQuizCount 이번 달 풀이한 퀴즈 수
 */
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
