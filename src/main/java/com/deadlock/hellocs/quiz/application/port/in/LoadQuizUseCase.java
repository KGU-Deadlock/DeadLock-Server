package com.deadlock.hellocs.quiz.application.port.in;

import com.deadlock.hellocs.quiz.application.port.in.request.LoadQuizRequest;
import com.deadlock.hellocs.quiz.domain.Quiz;

import java.util.List;

public interface LoadQuizUseCase {
    List<Quiz> loadQuizzes(LoadQuizRequest request);
}
