package com.deadlock.hellocs.grading.application.strategy;

import com.deadlock.hellocs.grading.application.port.out.dto.GradingTarget;
import com.deadlock.hellocs.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.contract.QuizType;

public interface GradingStrategy {
    boolean supports(QuizType type);
    GradingItem grade(GradingTarget target, String userAnswer);
}
