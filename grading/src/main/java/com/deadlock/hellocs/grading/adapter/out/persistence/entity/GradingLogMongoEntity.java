package com.deadlock.hellocs.grading.adapter.out.persistence.entity;

import com.deadlock.hellocs.grading.domain.GradingItem;
import com.deadlock.hellocs.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.contract.QuizMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code grading_logs} 컬렉션에 저장되는 MongoDB 엔티티.
 */
@Document(collection = "grading_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingLogMongoEntity {
    @Id
    private String id;
    private Long userId;
    private QuizMode mode;
    private LocalDateTime solvedAt;
    private int totalCount;
    private int correctCount;
    private List<GradingItem> results;
    private List<String> topicNames;

    public GradingLog toDomain() {
        return GradingLog.builder()
                .id(id)
                .userId(userId)
                .mode(mode)
                .solvedAt(solvedAt)
                .totalCount(totalCount)
                .correctCount(correctCount)
                .results(results)
                .topicNames(topicNames)
                .build();
    }

    public static GradingLogMongoEntity from(GradingLog gradingLog) {
        return GradingLogMongoEntity.builder()
                .id(gradingLog.getId())
                .userId(gradingLog.getUserId())
                .mode(gradingLog.getMode())
                .solvedAt(gradingLog.getSolvedAt())
                .totalCount(gradingLog.getTotalCount())
                .correctCount(gradingLog.getCorrectCount())
                .results(gradingLog.getResults())
                .topicNames(gradingLog.getTopicNames())
                .build();
    }
}
