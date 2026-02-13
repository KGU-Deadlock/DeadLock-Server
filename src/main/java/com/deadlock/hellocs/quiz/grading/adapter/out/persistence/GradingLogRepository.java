package com.deadlock.hellocs.quiz.grading.adapter.out.persistence;

import com.deadlock.hellocs.quiz.grading.adapter.out.persistence.entity.GradingLogMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GradingLogRepository extends MongoRepository<GradingLogMongoEntity, String> {
}
