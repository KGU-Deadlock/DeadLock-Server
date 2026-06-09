package com.deadlock.hellocs.quiz.application.policy;

import com.deadlock.hellocs.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import com.deadlock.hellocs.quiz.contract.QuizMode;

import java.util.List;

public interface QuizGenerationPolicy {
    boolean supports(QuizMode mode);
    List<Quiz> generate(GetQuizCommand command, QueryQuizOutputPort queryQuizPort, Long userId, QuizLevel level);
}
