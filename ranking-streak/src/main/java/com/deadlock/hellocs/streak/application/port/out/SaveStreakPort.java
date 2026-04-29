package com.deadlock.hellocs.streak.application.port.out;

import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;

/**
 * 스트릭 데이터를 저장/갱신하는 아웃바운드 포트.
 */
public interface SaveStreakPort {

    /** 사용자 스트릭을 저장하고 저장된 도메인 객체를 반환함. */
    UserStreak saveUserStreak(UserStreak userStreak);

    /** 일일 스트릭 기록을 저장하고 저장된 도메인 객체를 반환함. */
    DailyStreakRecord saveDailyRecord(DailyStreakRecord record);
}
