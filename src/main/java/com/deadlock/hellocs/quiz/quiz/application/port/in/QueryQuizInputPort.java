package com.deadlock.hellocs.quiz.quiz.application.port.in;

import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;

import java.util.List;


public interface QueryQuizInputPort {
    List<Quiz> getQuizzes(GetQuizCommand request);
}
