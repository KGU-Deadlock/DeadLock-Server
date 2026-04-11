package com.deadlock.hellocs.quiz.grading.application.policy;


import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;

public interface GradingPolicy {
    boolean supports(QuizType type);
    GradingItem grade(Quiz quiz, String userAnswer);
}
