package com.deadlock.hellocs.ranking.adapter.in.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RabbitMQ {@code grading.completed} 라우팅 키로 수신되는 채점 완료 메시지.
 * quiz 모듈의 {@code GradingCompletedEvent}와 JSON 필드명이 동일합니다.
 */
public record GradingCompletedMessage(
        String gradingLogId,
        Long userId,
        LocalDateTime solvedAt,
        int quizCount,
        int totalScore,
        List<Long> topicIds
) {}
