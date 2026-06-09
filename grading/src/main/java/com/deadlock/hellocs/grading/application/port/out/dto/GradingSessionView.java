package com.deadlock.hellocs.grading.application.port.out.dto;

import com.deadlock.hellocs.quiz.contract.QuizMode;

import java.util.List;
import java.util.Map;

public record GradingSessionView(
        QuizMode mode,
        List<Long> topicIds,
        Map<Long, GradingTarget> targets
) {
}
