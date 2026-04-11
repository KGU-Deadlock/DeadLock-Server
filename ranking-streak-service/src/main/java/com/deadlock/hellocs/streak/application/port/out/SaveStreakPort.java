package com.deadlock.hellocs.streak.application.port.out;

import com.deadlock.hellocs.streak.domain.UserStreak;

public interface SaveStreakPort {
    UserStreak save(UserStreak userStreak);
}
