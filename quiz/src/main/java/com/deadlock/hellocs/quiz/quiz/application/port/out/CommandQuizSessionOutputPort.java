package com.deadlock.hellocs.quiz.quiz.application.port.out;

import com.deadlock.hellocs.quiz.shared.domain.QuizSession;

public interface CommandQuizSessionOutputPort {
    void save(QuizSession session);
}
