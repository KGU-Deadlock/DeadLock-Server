package com.deadlock.hellocs.quiz.grading.adapter.out.persistence;

import com.deadlock.hellocs.quiz.grading.adapter.out.persistence.entity.GradingLogMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GradingLogRepository extends MongoRepository<GradingLogMongoEntity, String> {
    List<GradingLogMongoEntity> findAllByUserId(Long userId);
}
