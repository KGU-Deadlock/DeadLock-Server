package com.deadlock.hellocs.streak.adapter.in.event;

import java.time.LocalDateTime;

/**
 * RabbitMQ {@code interview.completed} 라우팅 키로 수신되는 인터뷰 완료 메시지.
 * interview 모듈의 {@code InterviewCompletedEvent}와 JSON 필드명이 동일합니다.
 */
public record InterviewCompletedMessage(
        String interviewId,
        Long userId,
        LocalDateTime completedAt,
        int questionCount
) {}
