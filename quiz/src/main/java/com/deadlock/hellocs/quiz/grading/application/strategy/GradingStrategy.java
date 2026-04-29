package com.deadlock.hellocs.quiz.grading.application.strategy;

import com.deadlock.hellocs.quiz.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;

public interface GradingStrategy {
    boolean supports(QuizType type);
    GradingItem grade(GradingTarget target, String userAnswer);
}
