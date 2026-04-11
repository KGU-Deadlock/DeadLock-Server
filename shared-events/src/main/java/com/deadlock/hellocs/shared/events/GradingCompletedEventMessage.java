package com.deadlock.hellocs.shared.events;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka 토픽 "grading.completed" 으로 발행되는 채점 완료 이벤트 메시지.
 * core-service가 produce, ranking-streak-service가 consume.
 *
 * topicIds 포함으로 ranking/streak 서비스가 core-service DB를 직접 조회할 필요 없음.
 */
public record GradingCompletedEventMessage(
        String gradingLogId,
        Long userId,
        LocalDateTime solvedAt,
        int quizCount,
        int totalScore,
        List<Long> topicIds
) {
}
