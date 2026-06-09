package com.deadlock.hellocs.quiz.contract;

import java.util.List;

public interface QueryQuizSessionOutputPort {
    QuizSession findByUserIdAndQuizIds(Long userId, List<Long> quizIds);
}
