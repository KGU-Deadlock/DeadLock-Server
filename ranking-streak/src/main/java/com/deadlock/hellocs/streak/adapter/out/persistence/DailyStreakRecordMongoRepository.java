package com.deadlock.hellocs.streak.adapter.out.persistence;

import com.deadlock.hellocs.streak.adapter.out.persistence.entity.DailyStreakRecordMongoEntity;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 일일 스트릭 기록의 MongoDB 레포지토리.
 *
 * <p>Aggregation 파이프라인을 활용하여 월별 활동 일수·퀴즈 수 집계를 MongoDB에서 직접 수행함.</p>
 */
public interface DailyStreakRecordMongoRepository extends MongoRepository<DailyStreakRecordMongoEntity, String> {

    /** 특정 날짜의 일일 스트릭 기록을 조회함. */
    Optional<DailyStreakRecordMongoEntity> findByUserIdAndDate(Long userId, LocalDate date);

    /** 특정 기간(from~to)의 일일 스트릭 기록 목록을 조회함. */
    List<DailyStreakRecordMongoEntity> findByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);

    /**
     * 지정 기간 중 퀴즈를 1개 이상 푼 날의 수를 집계함.
     * quizCount > 0 인 문서만 카운트함.
     */
    @Aggregation(pipeline = {
            "{ $match: { userId: ?0, date: { $gte: ?1, $lte: ?2 }, quizCount: { $gt: 0 } } }",
            "{ $count: 'count' }"
    })
    Optional<Integer> countActiveDays(Long userId, LocalDate from, LocalDate to);

    /** 지정 기간 동안 풀이한 퀴즈의 총 합산 수를 집계함. */
    @Aggregation(pipeline = {
            "{ $match: { userId: ?0, date: { $gte: ?1, $lte: ?2 } } }",
            "{ $group: { _id: null, total: { $sum: '$quizCount' } } }",
            "{ $project: { _id: 0, total: 1 } }"
    })
    Optional<Integer> sumQuizCount(Long userId, LocalDate from, LocalDate to);
}
