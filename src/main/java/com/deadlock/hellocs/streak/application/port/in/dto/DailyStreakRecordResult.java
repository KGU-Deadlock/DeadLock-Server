package com.deadlock.hellocs.streak.application.port.in.dto;

public record DailyStreakRecordResult(
        // 수정 표시
        String date,
        // 수정 표시
        boolean solved,
        // 수정 표시
        int quizCount,
        // 수정 표시
        int streakAtEndOfDay
) {
}
