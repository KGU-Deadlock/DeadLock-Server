package com.deadlock.hellocs.quiz.application.port.in.dto;

import com.deadlock.hellocs.quiz.domain.QuizShortAnswer;

public record ShortAnswerQuizResult(
        Long id,
        String content
) {
    public static ShortAnswerQuizResult from(QuizShortAnswer quiz) {
        return new ShortAnswerQuizResult(
                quiz.getId(),
                quiz.getContent()
        );
    }
}
