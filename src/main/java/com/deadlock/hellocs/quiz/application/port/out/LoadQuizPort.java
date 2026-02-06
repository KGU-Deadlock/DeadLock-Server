package com.deadlock.hellocs.quiz.application.port.out;

import com.deadlock.hellocs.quiz.QuizLevel;
import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.domain.Quiz;

import java.util.List;

public interface LoadQuizPort {
    List<Quiz> findQuizzesByCriteria(QuizLevel level, List<Long> topicIds, QuizType type, int count);
    List<Quiz> findAllByIds(List<Long> quizIds);
}
