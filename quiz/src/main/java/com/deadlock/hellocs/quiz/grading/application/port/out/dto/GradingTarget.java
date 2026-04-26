package com.deadlock.hellocs.quiz.grading.application.port.out.dto;

import com.deadlock.hellocs.quiz.shared.domain.QuizType;

public record GradingTarget(
        Long id,
        QuizType type,
        String content,
        String correctAnswer,
        String explanation
) {
}
