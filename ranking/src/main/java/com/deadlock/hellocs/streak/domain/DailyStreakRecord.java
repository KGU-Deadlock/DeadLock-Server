package com.deadlock.hellocs.streak.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DailyStreakRecord {

    private String id;
    private Long userId;
    private LocalDate date;
    private int quizCount;
    private int streakAtEndOfDay;
    private Set<String> appliedGradingLogIds;
    private Long version;

    public static DailyStreakRecord create(Long userId, LocalDate date) {
        return DailyStreakRecord.builder()
                .userId(userId)
                .date(date)
                .quizCount(0)
                .streakAtEndOfDay(0)
                .appliedGradingLogIds(new HashSet<>())
                .build();
    }

    public boolean isAlreadyApplied(String gradingLogId) {
        return appliedGradingLogIds != null && appliedGradingLogIds.contains(gradingLogId);
    }

    public boolean hasSolved() {
        return quizCount > 0;
    }

    public void apply(
            String gradingLogId,
            int addedQuizCount,
            int streakAtEndOfDay
    ) {
        quizCount += addedQuizCount;
        this.streakAtEndOfDay = streakAtEndOfDay;
        appliedGradingLogIds.add(gradingLogId);
    }
}
