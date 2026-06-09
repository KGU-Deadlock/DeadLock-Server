package com.deadlock.hellocs.quiz.contract;

import java.util.List;
import java.util.Map;

public record QuizSession(
        Long userId,
        QuizMode mode,
        List<Long> topicIds,
        Map<Long, QuizSessionEntry> quizzes
) {
}
