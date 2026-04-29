package com.deadlock.hellocs.quiz.grading.application.port.out;

import com.deadlock.hellocs.quiz.grading.domain.GradingLog;

public interface CommandGradingLogOutputPort {
    GradingLog save(GradingLog gradingLog);
}
