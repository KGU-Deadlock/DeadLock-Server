package com.deadlock.hellocs.quiz.quiz.application.port.in.dto;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;

import java.util.List;

/**
 * Quiz 조회 요청 DTO
 */
public record LoadQuizRequest(
        QuizLevel level,
        List<Long> topicIds,
        QuizMode mode
) {
    public LoadQuizRequest {
        if (level == null) {
            throw new IllegalArgumentException("Quiz level cannot be null");
        }
        if (topicIds == null || topicIds.isEmpty()) {
            throw new IllegalArgumentException("Topic IDs cannot be empty");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Quiz mode cannot be null");
        }
    }
}
