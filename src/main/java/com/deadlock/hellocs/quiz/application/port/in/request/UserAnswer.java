package com.deadlock.hellocs.quiz.application.port.in.request;

public record UserAnswer(
        Long quizId,
        String answer
) {
}
