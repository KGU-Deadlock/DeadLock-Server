package com.deadlock.hellocs.streak.domain;

import java.util.List;

public record DailyStreakRecord(
        // 수정 표시
        boolean solved,
        // 수정 표시
        int quizCount,
        // 수정 표시
        int streakAtEndOfDay,
        // 수정 표시
        List<Long> topicIds,
        // 수정 표시
        List<String> appliedGradingLogIds
) {
}
