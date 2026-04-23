package com.deadlock.hellocs.quiz.grading.domain;

import lombok.Builder;

import java.util.List;

@Builder
public record GradingItem(
        Long quizId,
        int score,
        boolean isCorrect,
        String userAnswer,
        String feedback,
        List<String> missingKeywords,
        String improvedAnswer
) {
    public GradingItem {
        missingKeywords = missingKeywords == null ? List.of() : List.copyOf(missingKeywords);
    }
}
