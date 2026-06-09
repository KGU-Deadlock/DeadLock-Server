package com.deadlock.hellocs.grading.application.port.out;

import com.deadlock.hellocs.grading.domain.GradingLog;

import java.util.List;

public interface QueryGradingLogOutputPort {
    GradingLog findById(String id);
    List<GradingLog> findAllByUserId(Long userId);
}
