package com.deadlock.hellocs.quiz.grading.application.port.in;

import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import jakarta.validation.Valid;

public interface QueryGradingLogInputPort {
    GradingLogResult getGradingLog(Long requesterId, @Valid GetGradingLogCommand command);
    GradingDetailLogResult getGradingDetailLog(Long requesterId, @Valid GetGradingDetailLogCommand command);
}
