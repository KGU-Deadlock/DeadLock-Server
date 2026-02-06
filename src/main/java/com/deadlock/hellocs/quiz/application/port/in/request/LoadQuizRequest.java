package com.deadlock.hellocs.quiz.application.port.in.request;

import com.deadlock.hellocs.quiz.QuizLevel;
import com.deadlock.hellocs.quiz.application.port.in.QuizMode;

import java.util.List;

public record LoadQuizRequest(
        QuizLevel level,
        List<Long> topicIds,
        QuizMode mode
) {
}
