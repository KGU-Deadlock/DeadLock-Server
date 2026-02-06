package com.deadlock.hellocs.quiz.application.policy.grade;

import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.application.port.in.response.GradingResult;
import com.deadlock.hellocs.quiz.domain.Quiz;

public interface GradingPolicy {
    boolean supports(QuizType type);
    GradingResult grade(Quiz quiz, String userAnswer);
}
