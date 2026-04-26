package com.deadlock.hellocs.quiz.quiz.domain;

import com.deadlock.hellocs.quiz.shared.domain.QuizType;

public record QuizSessionEntry(
        Long quizId,
        QuizType type,
        String content,
        String correctAnswer,
        String explanation
) {
}
