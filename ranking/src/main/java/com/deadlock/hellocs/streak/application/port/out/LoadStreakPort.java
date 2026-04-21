package com.deadlock.hellocs.streak.application.port.out;

import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface LoadStreakPort {

    Optional<UserStreak> loadUserStreakByUserId(Long userId);

    Optional<DailyStreakRecord> loadDailyRecord(Long userId, LocalDate date);

    List<DailyStreakRecord> loadDailyRecordsBetween(Long userId, LocalDate from, LocalDate to);

    int countActiveDays(Long userId, YearMonth yearMonth);

    int sumQuizCount(Long userId, YearMonth yearMonth);
}
