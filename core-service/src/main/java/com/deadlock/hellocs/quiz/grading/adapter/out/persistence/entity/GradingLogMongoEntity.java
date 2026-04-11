package com.deadlock.hellocs.quiz.grading.adapter.out.persistence.entity;

import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

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
    private QuizMode quizMode;
    private List<String> topicNames;

    public GradingLog toDomain() {
        return GradingLog.builder()
                .id(id)
                .userId(userId)
                .solvedAt(solvedAt)
                .totalCount(totalCount)
                .correctCount(correctCount)
                .results(results)
                .quizMode(quizMode)
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
                .quizMode(gradingLog.getQuizMode())
                .topicNames(gradingLog.getTopicNames())
                .build();
    }
}
