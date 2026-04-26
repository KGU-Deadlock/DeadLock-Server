package com.deadlock.hellocs.quiz.quiz.domain;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;

import java.util.List;
import java.util.Map;

public record QuizSession(
        Long userId,
        QuizMode mode,
        List<Long> topicIds,
        Map<Long, QuizSessionEntry> quizzes
) {
}
