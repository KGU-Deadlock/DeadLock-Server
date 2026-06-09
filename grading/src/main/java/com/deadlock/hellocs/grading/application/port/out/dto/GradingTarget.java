package com.deadlock.hellocs.grading.application.port.out.dto;

import com.deadlock.hellocs.quiz.contract.QuizType;

public record GradingTarget(
        Long id,
        QuizType type,
        String content,
        String correctAnswer,
        String explanation
) {
}
