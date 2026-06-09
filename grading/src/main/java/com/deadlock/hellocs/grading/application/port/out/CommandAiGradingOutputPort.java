package com.deadlock.hellocs.grading.application.port.out;

import com.deadlock.hellocs.grading.application.port.out.dto.AiFeedback;
import com.deadlock.hellocs.grading.application.port.out.dto.GradingTarget;

public interface CommandAiGradingOutputPort {
    AiFeedback evaluate(GradingTarget target, String userAnswer);
}
