package com.deadlock.hellocs.grading.adapter.out.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채점 완료 시 발행되는 도메인 이벤트.
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
