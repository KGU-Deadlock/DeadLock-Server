package com.deadlock.hellocs.streak.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserStreak {

    private String id;
    private Long userId;
    private int currentStreak;
    private int longestStreak;
    private int totalSolved;
    private Set<Long> solvedTopicIds;
    private LocalDate lastSolvedDate;
    private Long version;

    public static UserStreak create(Long userId) {
        return UserStreak.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .totalSolved(0)
                .solvedTopicIds(new HashSet<>())
                .build();
    }

    public void applySolved(LocalDate solvedDate, int quizCount, Collection<Long> newTopicIds) {
        updateStreak(solvedDate);
        totalSolved += quizCount;
        if (newTopicIds != null) {
            solvedTopicIds.addAll(newTopicIds);
        }
    }

    private void updateStreak(LocalDate solvedDate) {
        if (solvedDate.equals(lastSolvedDate)) {
            return;
        }
        currentStreak = isContinuedStreak(solvedDate) ? currentStreak + 1 : 1;
        longestStreak = Math.max(longestStreak, currentStreak);
        lastSolvedDate = solvedDate;
    }

    private boolean isContinuedStreak(LocalDate solvedDate) {
        if(lastSolvedDate == null)
            return false;
        if(ChronoUnit.DAYS.between(lastSolvedDate, solvedDate) > 1)
            return false;
        return true;
    }

    public int getCurrentStreakDays(LocalDate today) {
        if (isContinuedStreak(today)){
            return 0;
        }
        return currentStreak;
    }

    public int getSolvedTopicCount() {
        return solvedTopicIds == null ? 0 : solvedTopicIds.size();
    }
}
