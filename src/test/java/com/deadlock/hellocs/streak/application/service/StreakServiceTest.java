package com.deadlock.hellocs.streak.application.service;

import com.deadlock.hellocs.quiz.grading.application.port.out.QueryGradingLogOutputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import com.deadlock.hellocs.streak.application.port.out.LoadStreakPort;
import com.deadlock.hellocs.streak.application.port.out.SaveStreakPort;
import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private LoadStreakPort loadStreakPort;

    @Mock
    private SaveStreakPort saveStreakPort;

    @Mock
    private QueryGradingLogOutputPort queryGradingLogOutputPort;

    @Mock
    private QueryQuizOutputPort queryQuizOutputPort;

    private StreakService streakService;

    @BeforeEach
    void setUp() {
        streakService = new StreakService(loadStreakPort, saveStreakPort, queryGradingLogOutputPort, queryQuizOutputPort);
    }

    @Test
    void summaryFieldsAreReturned() {
        LocalDate today = LocalDate.now();
        Map<LocalDate, DailyStreakRecord> records = new HashMap<>();
        records.put(today, new DailyStreakRecord(true, 3, 1, List.of(1L, 2L), List.of("log-1")));
        UserStreak userStreak = UserStreak.builder()
                .userId(1L)
                .currentStreak(1)
                .longestStreak(2)
                .totalSolved(3)
                .dailyRecords(records)
                .lastSolvedDate(today)
                .build();
        when(loadStreakPort.loadByUserId(1L)).thenReturn(Optional.of(userStreak));

        StreakSummaryResult result = streakService.getSummary(1L);

        assertEquals(1, result.currentStreakDays());
        assertEquals(3, result.solvedQuizCount());
        assertEquals(2, result.solvedTopicCount());
    }

    @Test
    void detailFieldsAreReturned() {
        LocalDate today = LocalDate.now();
        Map<LocalDate, DailyStreakRecord> records = new HashMap<>();
        records.put(today.minusDays(1), new DailyStreakRecord(true, 2, 1, List.of(1L), List.of("log-1")));
        records.put(today, new DailyStreakRecord(true, 3, 2, List.of(2L), List.of("log-2")));
        UserStreak userStreak = UserStreak.builder()
                .userId(1L)
                .currentStreak(2)
                .longestStreak(5)
                .totalSolved(5)
                .dailyRecords(records)
                .lastSolvedDate(today)
                .build();
        when(loadStreakPort.loadByUserId(1L)).thenReturn(Optional.of(userStreak));

        StreakDetailResult result = streakService.getDetail(1L);

        assertEquals(2, result.currentStreakDays());
        assertEquals(5, result.longestStreakDays());
        assertEquals(today, result.lastSolvedDate());
        assertEquals(true, result.solvedToday());
        assertEquals(2, result.activeDaysThisMonth());
        assertEquals(5, result.currentMonthSolvedQuizCount());
    }

    @Test
    void monthlyCalculationIsCorrect() {
        YearMonth month = YearMonth.of(2026, 3);
        Map<LocalDate, DailyStreakRecord> records = new HashMap<>();
        records.put(month.atDay(1), new DailyStreakRecord(true, 1, 1, List.of(), List.of("log-1")));
        records.put(month.atDay(2), new DailyStreakRecord(false, 0, 0, List.of(), List.of()));
        records.put(month.atDay(3), new DailyStreakRecord(true, 2, 1, List.of(), List.of("log-2")));
        UserStreak userStreak = UserStreak.builder()
                .userId(1L)
                .currentStreak(1)
                .longestStreak(2)
                .totalSolved(3)
                .dailyRecords(records)
                .lastSolvedDate(month.atDay(3))
                .build();
        when(loadStreakPort.loadByUserId(1L)).thenReturn(Optional.of(userStreak));

        StreakMonthlyResult result = streakService.getMonthly(1L, 2026, 3);

        assertEquals(2026, result.year());
        assertEquals(3, result.month());
        assertEquals("2026-03-01", result.days().get(0).date());
        assertEquals(1, result.days().get(0).streakAtEndOfDay());
        assertEquals(0, result.days().get(1).streakAtEndOfDay());
        assertEquals(1, result.days().get(2).streakAtEndOfDay());
    }

    @Test
    void streakResetsAfterMissedDay() {
        LocalDate today = LocalDate.now();
        UserStreak userStreak = UserStreak.builder()
                .userId(1L)
                .currentStreak(3)
                .longestStreak(5)
                .totalSolved(10)
                .dailyRecords(Map.of())
                .lastSolvedDate(today.minusDays(2))
                .build();
        when(loadStreakPort.loadByUserId(1L)).thenReturn(Optional.of(userStreak));

        StreakSummaryResult result = streakService.getSummary(1L);

        assertEquals(0, result.currentStreakDays());
    }
}
