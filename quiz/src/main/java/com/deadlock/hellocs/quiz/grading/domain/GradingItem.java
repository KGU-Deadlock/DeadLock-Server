package com.deadlock.hellocs.quiz.grading.domain;

import com.deadlock.hellocs.quiz.grading.application.strategy.GradingStrategy;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.Builder;

import java.util.List;

/**
 * 퀴즈 1개의 채점 결과. {@link GradingStrategy#grade} 에서 생성되며 {@link GradingLog}에 집계됨.
 */
@Builder
public record GradingItem(
        Long quizId,
        QuizType quizType,
        String quizContent,
        String correctAnswer,
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
