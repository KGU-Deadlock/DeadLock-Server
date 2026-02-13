package com.deadlock.hellocs.quiz.grading.application.port.in;

import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;

public interface QueryGradingLogInputPort {
    GradingLogResult getGradingLog(GetGradingLogCommand command);
    GradingDetailLogResult getGradingDetailLog(GetGradingDetailLogCommand command);
}
