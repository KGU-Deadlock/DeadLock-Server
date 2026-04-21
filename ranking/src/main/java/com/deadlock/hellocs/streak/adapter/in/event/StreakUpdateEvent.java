package com.deadlock.hellocs.streak.adapter.in.event;

import java.time.LocalDate;
import java.util.List;

public record StreakUpdateEvent(
        String gradingLogId,
        Long userId,
        LocalDate solvedDate,
        int quizCount,
        List<Long> topicIds
) {}
