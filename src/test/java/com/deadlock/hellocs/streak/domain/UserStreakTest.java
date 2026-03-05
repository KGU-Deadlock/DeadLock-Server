package com.deadlock.hellocs.streak.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserStreakTest {

    @Test
    void applySolvedIncreasesCurrentStreakOnConsecutiveDay() {
        UserStreak userStreak = UserStreak.create(42L);

        userStreak.applySolved(LocalDate.of(2026, 3, 1), 3);
        userStreak.applySolved(LocalDate.of(2026, 3, 2), 2);

        assertEquals(2, userStreak.getCurrentStreak());
        assertEquals(2, userStreak.getLongestStreak());
        assertEquals(5, userStreak.getTotalSolved());
    }

    @Test
    void applySolvedResetsCurrentStreakAfterGap() {
        UserStreak userStreak = UserStreak.create(42L);

        userStreak.applySolved(LocalDate.of(2026, 3, 1), 1);
        userStreak.applySolved(LocalDate.of(2026, 3, 3), 4);

        assertEquals(1, userStreak.getCurrentStreak());
        assertEquals(1, userStreak.getLongestStreak());
        assertEquals(5, userStreak.getTotalSolved());
    }

    @Test
    void applySolvedOnSameDayAccumulatesQuizCountWithoutIncreasingStreak() {
        UserStreak userStreak = UserStreak.create(42L);

        userStreak.applySolved(LocalDate.of(2026, 3, 2), 2);
        userStreak.applySolved(LocalDate.of(2026, 3, 2), 3);

        assertEquals(1, userStreak.getCurrentStreak());
        assertEquals(1, userStreak.getLongestStreak());
        assertEquals(5, userStreak.getTotalSolved());
        assertEquals(5, userStreak.getDailyRecords().get(LocalDate.of(2026, 3, 2)).quizCount());
    }
}
