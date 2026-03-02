package com.deadlock.hellocs.streak.application.port.in.dto;

import java.util.List;

public record StreakMonthlyResult(
        // 수정 표시
        int year,
        // 수정 표시
        int month,
        // 수정 표시
        List<DailyStreakRecordResult> days
) {
}
