package com.deadlock.hellocs.streak.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Builder
public class UserStreak {

    private String id;
    private Long userId;
    private int currentStreak;
    private int longestStreak;
    private int totalSolved;
    @Builder.Default
    private Map<LocalDate, DailyStreakRecord> dailyRecords = new HashMap<>();
    private LocalDate lastSolvedDate;
    private Long version;

    public static UserStreak create(Long userId) {
        return UserStreak.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .totalSolved(0)
                .dailyRecords(new HashMap<>())
                .build();
    }

    // 수정 표시
    public void applySolved(LocalDate solvedDate, int quizCount, String gradingLogId, List<Long> topicIds) {
        DailyStreakRecord todayRecord = dailyRecords.get(solvedDate);
        if (todayRecord != null && todayRecord.appliedGradingLogIds().contains(gradingLogId)) {
            return;
        }

        boolean solvedToday = todayRecord != null && todayRecord.solved();

        if (!solvedToday) {
            if (lastSolvedDate == null) {
                currentStreak = 1;
            } else {
                long daysBetween = ChronoUnit.DAYS.between(lastSolvedDate, solvedDate);
                if (daysBetween == 1) {
                    currentStreak += 1;
                } else if (daysBetween > 1) {
                    currentStreak = 1;
                }
            }

            longestStreak = Math.max(longestStreak, currentStreak);
            lastSolvedDate = solvedDate;
        }

        int updatedQuizCount = (todayRecord == null ? 0 : todayRecord.quizCount()) + quizCount;
        Set<Long> mergedTopicIds = new HashSet<>(todayRecord == null ? List.of() : todayRecord.topicIds());
        mergedTopicIds.addAll(topicIds);
        List<String> appliedGradingLogIds = new ArrayList<>(todayRecord == null ? List.of() : todayRecord.appliedGradingLogIds());
        appliedGradingLogIds.add(gradingLogId);

        dailyRecords.put(solvedDate, new DailyStreakRecord(
                true,
                updatedQuizCount,
                currentStreak,
                mergedTopicIds.stream().sorted().toList(),
                appliedGradingLogIds
        ));
        totalSolved += quizCount;
    }

    // 수정 표시
    public int getCurrentStreakDays(LocalDate today) {
        if (lastSolvedDate == null) {
            return 0;
        }

        long daysBetween = ChronoUnit.DAYS.between(lastSolvedDate, today);
        if (daysBetween > 1) {
            return 0;
        }
        return currentStreak;
    }

    // 수정 표시
    public int getSolvedTopicCount() {
        return dailyRecords.values().stream()
                .flatMap(record -> record.topicIds().stream())
                .collect(java.util.stream.Collectors.toSet())
                .size();
    }

    // 수정 표시
    public boolean isSolvedOn(LocalDate date) {
        DailyStreakRecord record = dailyRecords.get(date);
        return record != null && record.solved();
    }

    // 수정 표시
    public int countActiveDays(YearMonth yearMonth) {
        return (int) dailyRecords.entrySet().stream()
                .filter(entry -> YearMonth.from(entry.getKey()).equals(yearMonth))
                .filter(entry -> entry.getValue().solved())
                .count();
    }

    // 수정 표시
    public int countSolvedQuizCount(YearMonth yearMonth) {
        return dailyRecords.entrySet().stream()
                .filter(entry -> YearMonth.from(entry.getKey()).equals(yearMonth))
                .mapToInt(entry -> entry.getValue().quizCount())
                .sum();
    }
}
