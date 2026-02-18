package com.deadlock.hellocs.quiz.grading.application.port.in;

import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import jakarta.validation.Valid;

public interface QueryGradingLogInputPort {
    GradingLogResult getGradingLog(@Valid GetGradingLogCommand command);
    GradingDetailLogResult getGradingDetailLog(@Valid GetGradingDetailLogCommand command);
}
