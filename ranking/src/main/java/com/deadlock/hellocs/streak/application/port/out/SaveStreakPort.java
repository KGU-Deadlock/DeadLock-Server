package com.deadlock.hellocs.streak.application.port.out;

import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;

public interface SaveStreakPort {

    UserStreak saveUserStreak(UserStreak userStreak);

    DailyStreakRecord saveDailyRecord(DailyStreakRecord record);
}
