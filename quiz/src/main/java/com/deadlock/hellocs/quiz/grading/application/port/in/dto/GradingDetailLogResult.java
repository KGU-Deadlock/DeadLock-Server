package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import lombok.Builder;

import java.util.List;

@Builder
public record GradingDetailLogResult(
        Long quizId,
        int score,
        boolean isCorrect,
        String content,
        String quizType,
        String userAnswer,
        String correctAnswer,
        String feedback,
        List<String> missingKeywords,
        String improvedAnswer
) {
    public static GradingDetailLogResult from(GradingItem gradingItem) {
        return GradingDetailLogResult.builder()
                .quizId(gradingItem.quizId())
                .score(gradingItem.score())
                .isCorrect(gradingItem.isCorrect())
                .content(gradingItem.quizContent())
                .quizType(gradingItem.quizType().getDescription())
                .userAnswer(gradingItem.userAnswer())
                .correctAnswer(gradingItem.correctAnswer())
                .feedback(gradingItem.feedback())
                .missingKeywords(gradingItem.missingKeywords())
                .improvedAnswer(gradingItem.improvedAnswer())
                .build();
    }
}
