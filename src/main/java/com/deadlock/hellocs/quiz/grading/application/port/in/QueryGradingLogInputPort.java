package com.deadlock.hellocs.quiz.grading.application.port.in;

import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;

public interface QueryGradingLogInputPort {
    GradingLogResult getGradingLog(String gradingLogId);
    GradingDetailLogResult getGradingDetailLog(String gradingLogId, Long quizId);
}
