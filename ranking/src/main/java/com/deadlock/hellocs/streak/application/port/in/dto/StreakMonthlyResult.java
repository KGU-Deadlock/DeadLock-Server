package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 월별 스트릭 캘린더 응답 DTO.
 * 해당 연월의 모든 날짜를 포함하며, 기록이 없는 날은 미풀이 상태로 채워짐.
 *
 * @param year  조회 연도
 * @param month 조회 월
 * @param days  일자별 스트릭 기록 목록
 */
public record StreakMonthlyResult(
        @Schema(example = "2025")
        int year,

        @Schema(example = "12")
        int month,

        List<DailyStreakRecordResult> days
) {}
