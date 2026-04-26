package com.deadlock.hellocs.quiz.grading.adapter.out.persistence.entity;

import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code grading_logs} 컬렉션에 저장되는 MongoDB 엔티티. {@code toDomain()} / {@code from()}으로 도메인과 변환함.
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
    private LocalDateTime solvedAt;
    private int totalCount;
    private int correctCount;
    private List<GradingItem> results;
    private List<String> topicNames;

    public GradingLog toDomain() {
        return GradingLog.builder()
                .id(id)
                .userId(userId)
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
                .solvedAt(gradingLog.getSolvedAt())
                .totalCount(gradingLog.getTotalCount())
                .correctCount(gradingLog.getCorrectCount())
                .results(gradingLog.getResults())
                .topicNames(gradingLog.getTopicNames())
                .build();
    }
}
