package com.deadlock.hellocs.quiz.application.port.out;

import com.deadlock.hellocs.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import com.deadlock.hellocs.quiz.contract.QuizType;

import java.util.List;

public interface QueryQuizOutputPort {
    List<Quiz> findQuizzesByCriteria(QuizLevel level, List<Long> topicIds, QuizType type);
    Quiz findById(Long quizId);
    List<Quiz> findAllByIds(List<Long> quizIds);
}
