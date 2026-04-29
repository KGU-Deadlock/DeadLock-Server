package com.deadlock.hellocs.quiz.shared.domain;

public record QuizSessionEntry(
        Long quizId,
        QuizType type,
        String content,
        String correctAnswer,
        String explanation
) {
}
