package com.deadlock.hellocs.interview.session.application.event;

import java.time.LocalDateTime;

public record InterviewCompletedEvent(
        String interviewId,
        Long userId,
        LocalDateTime completedAt,
        int questionCount
) {}
