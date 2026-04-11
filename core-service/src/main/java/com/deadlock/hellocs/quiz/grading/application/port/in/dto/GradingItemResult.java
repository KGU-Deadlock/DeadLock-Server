package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import lombok.Builder;

@Builder
public record GradingItemResult(
        Long quizId,
        String content,
        String quizType,
        boolean isCorrect
) {
    public static GradingItemResult from(GradingItem gradingItem, Quiz quiz) {
        return GradingItemResult.builder()
                .quizId(gradingItem.quizId())
                .content(quiz.getContent())
                .quizType(quiz.getType().getDescription())
                .isCorrect(gradingItem.isCorrect())
                .build();
    }
}
