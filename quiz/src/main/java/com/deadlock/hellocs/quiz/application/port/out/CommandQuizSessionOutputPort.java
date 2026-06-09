package com.deadlock.hellocs.quiz.application.port.out;

import com.deadlock.hellocs.quiz.contract.QuizSession;

public interface CommandQuizSessionOutputPort {
    void save(QuizSession session);
}
