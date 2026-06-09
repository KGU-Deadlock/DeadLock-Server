package com.deadlock.hellocs.grading.application.port.out;

import com.deadlock.hellocs.grading.application.port.out.dto.GradingSessionView;

import java.util.List;

public interface QueryGradingTargetOutputPort {
    GradingSessionView fetchSession(Long userId, List<Long> quizIds);
}
