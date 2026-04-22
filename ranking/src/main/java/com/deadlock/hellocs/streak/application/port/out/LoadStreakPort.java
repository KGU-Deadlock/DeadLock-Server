package com.deadlock.hellocs.streak.application.port.out;

import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 스트릭 데이터를 읽어오는 아웃바운드 포트.
 *
 * <p>현재 구현은 MongoDB({@link com.deadlock.hellocs.streak.adapter.out.persistence.StreakPersistenceAdapter})이며,
 * 서비스는 이 인터페이스만 의존하므로 저장소 교체 시 서비스 코드는 변경되지 않음.</p>
 */
public interface LoadStreakPort {

    /** userId로 사용자 스트릭을 조회함. 존재하지 않으면 {@link Optional#empty()} 반환함. */
    Optional<UserStreak> loadUserStreakByUserId(Long userId);

    /** 특정 날짜의 일일 스트릭 기록을 조회함. */
    Optional<DailyStreakRecord> loadDailyRecord(Long userId, LocalDate date);

    /** 특정 기간(from~to)의 일일 스트릭 기록 목록을 조회함. */
    List<DailyStreakRecord> loadDailyRecordsBetween(Long userId, LocalDate from, LocalDate to);

    /** 지정 연월의 활동(퀴즈 1개 이상 풀이) 일수를 반환함. */
    int countActiveDays(Long userId, YearMonth yearMonth);

    /** 지정 연월의 총 풀이 퀴즈 수를 반환함. */
    int sumQuizCount(Long userId, YearMonth yearMonth);
}
