package com.deadlock.hellocs.quiz.quiz.application.port.in.dto;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;

import java.util.List;

/**
 * Quiz 조회 요청 DTO
 */
public record GetQuizCommand(
        QuizLevel level,
        List<Long> topicIds,
        QuizMode mode
) {
}
