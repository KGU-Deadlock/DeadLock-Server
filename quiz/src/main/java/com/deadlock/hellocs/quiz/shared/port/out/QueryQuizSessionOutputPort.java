package com.deadlock.hellocs.quiz.shared.port.out;

import com.deadlock.hellocs.quiz.shared.domain.QuizSession;

import java.util.List;

public interface QueryQuizSessionOutputPort {
    QuizSession findByUserIdAndQuizIds(Long userId, List<Long> quizIds);
}
