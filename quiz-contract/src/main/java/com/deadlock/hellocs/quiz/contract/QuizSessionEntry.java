package com.deadlock.hellocs.quiz.contract;

public record QuizSessionEntry(
        Long quizId,
        QuizType type,
        String content,
        String correctAnswer,
        String explanation
) {
}
