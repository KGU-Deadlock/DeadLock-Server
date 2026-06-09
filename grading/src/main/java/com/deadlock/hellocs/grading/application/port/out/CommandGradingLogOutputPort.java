package com.deadlock.hellocs.grading.application.port.out;

import com.deadlock.hellocs.grading.domain.GradingLog;

public interface CommandGradingLogOutputPort {
    GradingLog save(GradingLog gradingLog);
}
