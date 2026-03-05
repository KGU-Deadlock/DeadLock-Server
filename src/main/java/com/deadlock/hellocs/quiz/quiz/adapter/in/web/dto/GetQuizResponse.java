package com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto;

import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;

public record GetQuizResponse(
        Long id,
        String content,
        QuizType type
) {
    public static GetQuizResponse from(Quiz quiz) {
        return new GetQuizResponse(quiz.getId(), quiz.getContent(), quiz.getType());
    }
}
