package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
public record GradingLogResult(
        Integer correctCount,
        Integer quizCount,
        List<GradingItemResult> gradingResults
) {
    public static GradingLogResult from(GradingLog gradingLog, List<Quiz> quizzes) {
        Map<Long, Quiz> quizMap = quizzes.stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));

        List<GradingItemResult> results = gradingLog.getResults().stream()
                .map(item -> GradingItemResult.from(item, quizMap.get(item.quizId())))
                .toList();

        return GradingLogResult.builder()
                .correctCount(gradingLog.getCorrectCount())
                .quizCount(gradingLog.getTotalCount())
                .gradingResults(results)
                .build();
    }
}
