package com.deadlock.hellocs.quiz.grading.application.event;

import java.time.LocalDateTime;

public record GradingCompletedEvent(
        String gradingLogId,
        Long userId,
        LocalDateTime solvedAt,
        int quizCount,
        int totalScore
) {
}
