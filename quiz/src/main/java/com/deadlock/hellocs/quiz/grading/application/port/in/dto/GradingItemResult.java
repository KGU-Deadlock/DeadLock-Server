package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import lombok.Builder;

@Builder
public record GradingItemResult(
        Long quizId,
        String content,
        String quizType,
        boolean isCorrect
) {
    public static GradingItemResult from(GradingItem gradingItem) {
        return GradingItemResult.builder()
                .quizId(gradingItem.quizId())
                .content(gradingItem.quizContent())
                .quizType(gradingItem.quizType().getDescription())
                .isCorrect(gradingItem.isCorrect())
                .build();
    }
}
