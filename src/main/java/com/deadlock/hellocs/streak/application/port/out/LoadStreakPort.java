package com.deadlock.hellocs.streak.application.port.out;

import com.deadlock.hellocs.streak.domain.UserStreak;

import java.util.Optional;

public interface LoadStreakPort {
    Optional<UserStreak> loadByUserId(Long userId);
}
