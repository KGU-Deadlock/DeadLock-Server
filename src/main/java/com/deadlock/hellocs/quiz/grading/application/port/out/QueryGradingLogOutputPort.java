package com.deadlock.hellocs.quiz.grading.application.port.out;

import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import java.util.Optional;

public interface QueryGradingLogOutputPort {
    GradingLog findById(String id);
}
