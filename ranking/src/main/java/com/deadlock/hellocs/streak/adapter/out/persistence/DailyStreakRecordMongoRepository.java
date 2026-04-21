package com.deadlock.hellocs.streak.adapter.out.persistence;

import com.deadlock.hellocs.streak.adapter.out.persistence.entity.DailyStreakRecordMongoEntity;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyStreakRecordMongoRepository extends MongoRepository<DailyStreakRecordMongoEntity, String> {

    Optional<DailyStreakRecordMongoEntity> findByUserIdAndDate(Long userId, LocalDate date);

    List<DailyStreakRecordMongoEntity> findByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);

    @Aggregation(pipeline = {
            "{ $match: { userId: ?0, date: { $gte: ?1, $lte: ?2 }, quizCount: { $gt: 0 } } }",
            "{ $count: 'count' }"
    })
    Optional<Integer> countActiveDays(Long userId, LocalDate from, LocalDate to);

    @Aggregation(pipeline = {
            "{ $match: { userId: ?0, date: { $gte: ?1, $lte: ?2 } } }",
            "{ $group: { _id: null, total: { $sum: '$quizCount' } } }",
            "{ $project: { _id: 0, total: 1 } }"
    })
    Optional<Integer> sumQuizCount(Long userId, LocalDate from, LocalDate to);
}
