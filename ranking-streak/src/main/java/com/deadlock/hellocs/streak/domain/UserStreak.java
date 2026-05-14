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

/**
 * 사용자 전체 스트릭 통계 도메인 모델.
 *
 * <p>현재 연속 일수, 역대 최장 연속 일수, 총 풀이 수, 풀이 분야 집합을 관리함.
 * 스트릭 연속 여부는 {@code lastSolvedDate}와 새 풀이일의 간격이 1일 이하일 때 유지되며,
 * 초과하면 1로 초기화됨.</p>
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserStreak {

    private String id;
    private Long userId;
    /** 현재 연속 학습 일수. 하루라도 건너뛰면 1로 초기화됨. */
    private int currentStreak;
    /** 역대 최장 연속 학습 일수. */
    private int longestStreak;
    /** 서비스 가입 이후 총 누적 풀이 수. */
    private int totalSolved;
    /** 풀이한 적 있는 주제(topic) ID 집합. */
    private Set<Long> solvedTopicIds;
    /** 가장 최근에 퀴즈를 풀이한 날짜. 스트릭 연속 여부 판단에 사용됨. */
    private LocalDate lastSolvedDate;
    private Long version;

    /** 주어진 userId로 모든 카운터가 0인 새 UserStreak을 생성함. */
    public static UserStreak create(Long userId) {
        return UserStreak.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .totalSolved(0)
                .solvedTopicIds(new HashSet<>())
                .build();
    }

    /**
     * 퀴즈 풀이 결과를 스트릭에 반영함.
     * 연속 일수를 갱신하고, totalSolved에 quizCount를 가산하며, 주제 집합을 확장함.
     */
    public void applySolved(LocalDate solvedDate, int quizCount, Collection<Long> newTopicIds) {
        updateStreak(solvedDate);
        totalSolved += quizCount;
        if (newTopicIds != null) {
            solvedTopicIds.addAll(newTopicIds);
        }
    }

    /**
     * 연속 스트릭 카운터를 갱신함.
     * 동일 날짜 재반영이면 스킵하고, 연속이면 +1, 아니면 1로 초기화함.
     */
    private void updateStreak(LocalDate solvedDate) {
        if (solvedDate.equals(lastSolvedDate)) {
            return;
        }
        currentStreak = isContinuedStreak(solvedDate) ? currentStreak + 1 : 1;
        longestStreak = Math.max(longestStreak, currentStreak);
        lastSolvedDate = solvedDate;
    }

    /** 직전 풀이일로부터 1일 이하 간격이면 연속 스트릭으로 판정함. lastSolvedDate가 null이면 false 반환함. */
    private boolean isContinuedStreak(LocalDate solvedDate) {
        if(lastSolvedDate == null)
            return false;
        if(ChronoUnit.DAYS.between(lastSolvedDate, solvedDate) > 1)
            return false;
        return true;
    }

    /** today 기준으로 스트릭이 아직 유효한지 확인 후 현재 스트릭 일수를 반환함. */
    public int getCurrentStreakDays(LocalDate today) {
        if (isContinuedStreak(today)){
            return currentStreak;
        }
        return 0;
    }

    /** 풀이한 주제 수를 반환함. solvedTopicIds가 null이면 0을 반환함. */
    public int getSolvedTopicCount() {
        return solvedTopicIds == null ? 0 : solvedTopicIds.size();
    }
}
