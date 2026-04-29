package com.deadlock.hellocs.quiz.quiz.application.port.out;

import com.deadlock.hellocs.quiz.quiz.domain.QuizSession;

import java.util.List;

public interface QueryQuizSessionOutputPort {
    QuizSession findByUserIdAndQuizIds(Long userId, List<Long> quizIds);
}
