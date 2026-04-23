package com.deadlock.hellocs.quiz.quiz.application.port.in.dto;

import com.deadlock.hellocs.quiz.quiz.domain.QuizOx;

public record OxQuizResult(
        Long id,
        String content
) {
    public static OxQuizResult from(QuizOx quiz) {
        return new OxQuizResult(
                quiz.getId(),
                quiz.getContent()
        );
    }
}
