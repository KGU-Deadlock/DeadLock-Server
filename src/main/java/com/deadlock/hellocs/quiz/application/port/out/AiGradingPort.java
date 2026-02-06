package com.deadlock.hellocs.quiz.application.port.out;

import com.deadlock.hellocs.quiz.application.port.in.response.GradingResult;
import com.deadlock.hellocs.quiz.domain.Quiz;

public interface AiGradingPort {
    GradingResult gradeWithAi(Quiz quiz, String userAnswer);
}
