package com.deadlock.hellocs.streak.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * 하루 단위 퀴즈 풀이 기록 도메인 모델.
 *
 * <p>사용자별·날짜별로 단 하나의 기록이 존재하며,
 * 당일 풀이한 퀴즈 수와 하루가 끝날 시점의 연속 스트릭 일수를 누적 관리함.
 * 동일한 채점 로그가 중복으로 반영되지 않도록 {@code appliedGradingLogIds}로 멱등성을 보장함.</p>
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DailyStreakRecord {

    private String id;
    private Long userId;
    private LocalDate date;
    /** 당일 풀이한 퀴즈 수 (누적 가산됨). */
    private int quizCount;
    /** 당일 하루가 끝날 시점의 연속 스트릭 일수. */
    private int streakAtEndOfDay;
    /** 중복 채점 로그 방지용 — 이미 반영된 gradingLogId를 관리함. */
    private Set<String> appliedGradingLogIds;
    private Long version;

    /** 주어진 userId와 date로 초기값이 0인 새 일일 기록을 생성함. */
    public static DailyStreakRecord create(Long userId, LocalDate date) {
        return DailyStreakRecord.builder()
                .userId(userId)
                .date(date)
                .quizCount(0)
                .streakAtEndOfDay(0)
                .appliedGradingLogIds(new HashSet<>())
                .build();
    }

    /** 이미 반영된 채점 로그 ID인지 확인함. 중복 반영 방지에 사용됨. */
    public boolean isAlreadyApplied(String gradingLogId) {
        return appliedGradingLogIds != null && appliedGradingLogIds.contains(gradingLogId);
    }

    /** 당일 퀴즈를 1개 이상 풀었으면 true를 반환함. */
    public boolean hasSolved() {
        return quizCount > 0;
    }

    /**
     * 채점 결과를 일일 기록에 반영함.
     * quizCount에 addedQuizCount를 누적하고, streakAtEndOfDay를 갱신하며, gradingLogId를 기록함.
     */
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
