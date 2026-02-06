package com.deadlock.hellocs.quiz.application.policy.quiz;

import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.application.port.in.QuizMode;

import java.util.Map;

public interface QuizGenerationPolicy {
    boolean supports(QuizMode mode);
    Map<QuizType, Integer> getQuizComposition();
}
