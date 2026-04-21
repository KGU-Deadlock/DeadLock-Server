package com.deadlock.hellocs.streak.adapter.out.persistence;

import com.deadlock.hellocs.streak.adapter.out.persistence.entity.DailyStreakRecordMongoEntity;
import com.deadlock.hellocs.streak.adapter.out.persistence.entity.UserStreakMongoEntity;
import com.deadlock.hellocs.streak.application.port.out.LoadStreakPort;
import com.deadlock.hellocs.streak.application.port.out.SaveStreakPort;
import com.deadlock.hellocs.streak.domain.DailyStreakRecord;
import com.deadlock.hellocs.streak.domain.UserStreak;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StreakPersistenceAdapter implements LoadStreakPort, SaveStreakPort {

    private final UserStreakMongoRepository userStreakMongoRepository;
    private final DailyStreakRecordMongoRepository dailyStreakRecordMongoRepository;

    @Override
    public Optional<UserStreak> loadUserStreakByUserId(Long userId) {
        return userStreakMongoRepository.findByUserId(userId)
                .map(UserStreakMongoEntity::toDomain);
    }

    @Override
    public Optional<DailyStreakRecord> loadDailyRecord(Long userId, LocalDate date) {
        return dailyStreakRecordMongoRepository.findByUserIdAndDate(userId, date)
                .map(DailyStreakRecordMongoEntity::toDomain);
    }

    @Override
    public List<DailyStreakRecord> loadDailyRecordsBetween(Long userId, LocalDate from, LocalDate to) {
        return dailyStreakRecordMongoRepository.findByUserIdAndDateBetween(userId, from, to).stream()
                .map(DailyStreakRecordMongoEntity::toDomain)
                .toList();
    }

    @Override
    public int countActiveDays(Long userId, YearMonth yearMonth) {
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();
        return dailyStreakRecordMongoRepository.countActiveDays(userId, from, to).orElse(0);
    }

    @Override
    public int sumQuizCount(Long userId, YearMonth yearMonth) {
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();
        return dailyStreakRecordMongoRepository.sumQuizCount(userId, from, to).orElse(0);
    }

    @Override
    public UserStreak saveUserStreak(UserStreak userStreak) {
        UserStreakMongoEntity saved = userStreakMongoRepository.save(UserStreakMongoEntity.from(userStreak));
        return saved.toDomain();
    }

    @Override
    public DailyStreakRecord saveDailyRecord(DailyStreakRecord record) {
        DailyStreakRecordMongoEntity saved = dailyStreakRecordMongoRepository.save(DailyStreakRecordMongoEntity.from(record));
        return saved.toDomain();
    }
}
