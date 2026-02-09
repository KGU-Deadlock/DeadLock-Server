package com.deadlock.hellocs.quiz.grading.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Document(collection = "grading_logs")
public class GradingLog {
    @Id
    private String id;
    private Long userId;
    private LocalDateTime solvedAt;
    private int totalCount;
    private int correctCount;
    private List<GradingItem> results;

    public static GradingLog create(Long userId, List<GradingItem> results) {
        int correctCount = (int) results.stream()
                .filter(GradingItem::isCorrect)
                .count();

        return GradingLog.builder()
                .userId(userId)
                .solvedAt(LocalDateTime.now())
                .totalCount(results.size())
                .correctCount(correctCount)
                .results(results)
                .build();
    }
}
