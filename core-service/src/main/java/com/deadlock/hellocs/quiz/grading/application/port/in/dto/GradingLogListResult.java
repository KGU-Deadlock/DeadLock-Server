package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

import com.deadlock.hellocs.quiz.grading.domain.GradingLog;

import java.time.LocalDateTime;
import java.util.List;

public record GradingLogListResult(
        String id,
        LocalDateTime solvedAt,
        int correctCount,
        int totalCount,
        String quizMode,
        List<String> topicNames
) {
    public static GradingLogListResult from(GradingLog gradingLog) {
        String mode = gradingLog.getQuizMode() != null
                ? gradingLog.getQuizMode().name()
                : null;

        List<String> topics = gradingLog.getTopicNames() != null
                ? gradingLog.getTopicNames()
                : List.of();

        return new GradingLogListResult(
                gradingLog.getId(),
                gradingLog.getSolvedAt(),
                gradingLog.getCorrectCount(),
                gradingLog.getTotalCount(),
                mode,
                topics
        );
    }
}
