package com.deadlock.hellocs.quiz.grading.application.port.in;


import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserAnswer;
import com.deadlock.hellocs.quiz.grading.domain.GradingResult;

import java.util.List;

public interface SubmitAnswerInputPort {
    List<GradingResult> submitAnswers(List<UserAnswer> answers);
}
