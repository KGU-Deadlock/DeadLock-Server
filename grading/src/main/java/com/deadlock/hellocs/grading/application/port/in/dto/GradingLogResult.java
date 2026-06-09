package com.deadlock.hellocs.grading.application.port.in.dto;

import com.deadlock.hellocs.grading.domain.GradingLog;
import lombok.Builder;

import java.util.List;

@Builder
public record GradingLogResult(
        Integer correctCount,
        Integer quizCount,
        List<GradingItemResult> gradingResults
) {
    public static GradingLogResult from(GradingLog gradingLog) {
        List<GradingItemResult> results = gradingLog.getResults().stream()
                .map(GradingItemResult::from)
                .toList();

        return GradingLogResult.builder()
                .correctCount(gradingLog.getCorrectCount())
                .quizCount(gradingLog.getTotalCount())
                .gradingResults(results)
                .build();
    }
}
