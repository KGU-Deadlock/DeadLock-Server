package com.deadlock.hellocs.quiz.grading.domain;

import lombok.Builder;

@Builder
public record GradingItem(
        Long quizId,
        int score,
        boolean isCorrect,
        String userAnswer,
        String feedback
) {
}
