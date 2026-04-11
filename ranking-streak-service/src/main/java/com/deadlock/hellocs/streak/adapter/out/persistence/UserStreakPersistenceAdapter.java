package com.deadlock.hellocs.streak.adapter.out.persistence;

import com.deadlock.hellocs.streak.adapter.out.persistence.entity.UserStreakMongoEntity;
import com.deadlock.hellocs.streak.application.port.out.LoadStreakPort;
import com.deadlock.hellocs.streak.application.port.out.SaveStreakPort;
import com.deadlock.hellocs.streak.domain.UserStreak;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserStreakPersistenceAdapter implements LoadStreakPort, SaveStreakPort {

    private final UserStreakRepository userStreakRepository;

    @Override
    public Optional<UserStreak> loadByUserId(Long userId) {
        return userStreakRepository.findByUserId(userId)
                .map(UserStreakMongoEntity::toDomain);
    }

    @Override
    public UserStreak save(UserStreak userStreak) {
        return userStreakRepository.save(UserStreakMongoEntity.from(userStreak)).toDomain();
    }
}
