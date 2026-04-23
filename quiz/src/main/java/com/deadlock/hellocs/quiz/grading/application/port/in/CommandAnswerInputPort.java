package com.deadlock.hellocs.quiz.grading.application.port.in;

import com.deadlock.hellocs.quiz.grading.application.port.in.dto.SubmitAnswersCommand;
import jakarta.validation.Valid;

public interface CommandAnswerInputPort {
    String submit(@Valid SubmitAnswersCommand command);
}
