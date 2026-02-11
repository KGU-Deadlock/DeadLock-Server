package com.deadlock.hellocs.quiz.grading.application.port.in;

import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;

import java.util.List;

public interface CommandAnswerInputPort {
    String submit(Long userId, List<UserGradingCommand> answers);
}
