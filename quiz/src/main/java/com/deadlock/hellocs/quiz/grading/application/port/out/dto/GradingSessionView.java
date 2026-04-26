package com.deadlock.hellocs.quiz.grading.application.port.out.dto;

import java.util.List;
import java.util.Map;

public record GradingSessionView(
        List<Long> topicIds,
        List<String> topicNames,
        Map<Long, GradingTarget> targets
) {
}
