package com.deadlock.hellocs.quiz.quiz.application.port.in;

import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public interface QueryQuizInputPort {
    GetQuizResult getQuizzes(@NotNull @Valid GetQuizCommand request);
}
