package com.deadlock.hellocs.quiz.grading.application.port.out.dto;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;

import java.util.List;
import java.util.Map;

public record GradingSessionView(
        QuizMode mode,
        List<Long> topicIds,
        List<String> topicNames,
        Map<Long, GradingTarget> targets
) {
}
