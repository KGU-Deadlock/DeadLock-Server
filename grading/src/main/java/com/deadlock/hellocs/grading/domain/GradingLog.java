package com.deadlock.hellocs.grading.domain;

import com.deadlock.hellocs.quiz.contract.QuizMode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 한 번의 채점 세션 결과를 담는 도메인 엔티티.
 */
@Getter
@Builder
public class GradingLog {
    private String id;
    private Long userId;
    private QuizMode mode;
    private LocalDateTime solvedAt;
    private int totalCount;
    private int correctCount;
    private List<GradingItem> results;
    private List<String> topicNames;

    /** 정답 수를 자동 집계하여 채점 로그를 생성하는 팩토리 메서드. */
    public static GradingLog create(Long userId, QuizMode mode, List<GradingItem> results, List<String> topicNames) {
        int correctCount = (int) results.stream()
                .filter(GradingItem::isCorrect)
                .count();

        return GradingLog.builder()
                .userId(userId)
                .mode(mode)
                .solvedAt(LocalDateTime.now())
                .totalCount(results.size())
                .correctCount(correctCount)
                .results(results)
                .topicNames(topicNames)
                .build();
    }
}
