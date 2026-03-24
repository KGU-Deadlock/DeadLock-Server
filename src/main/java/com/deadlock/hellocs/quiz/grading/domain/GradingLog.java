package com.deadlock.hellocs.quiz.grading.domain;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class GradingLog {
    private String id;
    private Long userId;
    private LocalDateTime solvedAt;
    private int totalCount;
    private int correctCount;
    private List<GradingItem> results;
    private QuizMode quizMode;
    private List<String> topicNames;

    public static GradingLog create(Long userId, List<GradingItem> results,
                                    QuizMode quizMode, List<String> topicNames) {
        int correctCount = (int) results.stream()
                .filter(GradingItem::isCorrect)
                .count();

        return GradingLog.builder()
                .userId(userId)
                .solvedAt(LocalDateTime.now())
                .totalCount(results.size())
                .correctCount(correctCount)
                .results(results)
                .quizMode(quizMode)
                .topicNames(topicNames)
                .build();
    }
}
