package com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;

import java.util.List;

public record GetQuizRequest(
        List<Long> topicIds,
        QuizMode mode
) {
}
