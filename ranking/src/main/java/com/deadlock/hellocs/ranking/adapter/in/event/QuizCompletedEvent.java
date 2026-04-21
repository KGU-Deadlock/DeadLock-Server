package com.deadlock.hellocs.ranking.adapter.in.event;

import java.util.List;

public record QuizCompletedEvent(
        Long userId,
        List<Long> topicIds,
        int score
) {}
