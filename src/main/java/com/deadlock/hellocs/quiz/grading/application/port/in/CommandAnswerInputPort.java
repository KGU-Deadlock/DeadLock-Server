package com.deadlock.hellocs.quiz.grading.application.port.in;

import com.deadlock.hellocs.quiz.grading.application.port.in.dto.SubmitAnswersCommand;

public interface CommandAnswerInputPort {
    String submit(SubmitAnswersCommand command);
}
