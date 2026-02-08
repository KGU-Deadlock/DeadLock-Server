package com.deadlock.hellocs.quiz.quiz.application.port.out;

import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;

import java.util.List;
import java.util.Optional;

public interface LoadQuizOutputPort {
    List<Quiz> findQuizzesByCriteria(
            QuizLevel level,
            List<Long> topicIds,
            QuizType type,
            int count
    );
    Quiz findById(Long quizId);
    List<Quiz> findAllByIds(List<Long> quizIds);
}
