package com.deadlock.hellocs.quiz.application.port.in;

import com.deadlock.hellocs.quiz.application.port.in.request.UserAnswer;
import com.deadlock.hellocs.quiz.application.port.in.response.GradingResult;

import java.util.List;

public interface SubmitQuizUseCase {
    List<GradingResult> submitAnswers(List<UserAnswer> answers);
}
