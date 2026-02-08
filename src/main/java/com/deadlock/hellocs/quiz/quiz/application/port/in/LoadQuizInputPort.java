package com.deadlock.hellocs.quiz.quiz.application.port.in;

import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.LoadQuizRequest;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;

import java.util.List;


public interface LoadQuizInputPort {
    List<Quiz> loadQuizzes(LoadQuizRequest request);
}
