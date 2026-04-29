package com.deadlock.hellocs.quiz.shared.domain;

import java.util.List;
import java.util.Map;

public record QuizSession(
        Long userId,
        QuizMode mode,
        List<Long> topicIds,
        Map<Long, QuizSessionEntry> quizzes
) {
}
