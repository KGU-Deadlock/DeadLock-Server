package com.deadlock.hellocs.quiz.grading.application.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채점 완료 시 발행되는 도메인 이벤트.
 * <p>{@code RankingGradingCompletedEventListener}, {@code StreakGradingCompletedEventListener}가 구독하여 랭킹·스트릭을 갱신함.</p>
 */
public record GradingCompletedEvent(
        String gradingLogId,
        Long userId,
        LocalDateTime solvedAt,
        int quizCount,
        int totalScore,
        List<Long> topicIds
) {
}
