package com.deadlock.hellocs.quiz.quiz.application.port.in;

import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public interface QueryQuizInputPort {
    List<Quiz> getQuizzes(@NotNull @Valid GetQuizCommand request);
}
