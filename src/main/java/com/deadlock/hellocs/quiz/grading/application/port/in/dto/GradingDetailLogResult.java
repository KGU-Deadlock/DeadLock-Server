package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import lombok.Builder;

@Builder
public record GradingDetailLogResult(
        Long quizId,
        int score,
        boolean isCorrect,
        String content,
        String quizType,
        String userAnswer,
        String correctAnswer,
        String feedback
) {
    public static GradingDetailLogResult from(GradingItem gradingItem, Quiz quiz) {
        return GradingDetailLogResult.builder()
                .quizId(gradingItem.quizId())
                .score(gradingItem.score())
                .isCorrect(gradingItem.isCorrect())
                .content(quiz.getContent())
                .quizType(quiz.getType().getDescription())
                .userAnswer(gradingItem.userAnswer())
                .correctAnswer(quiz.getAnswer().asString())
                .feedback(gradingItem.feedback())
                .build();
    }
}
