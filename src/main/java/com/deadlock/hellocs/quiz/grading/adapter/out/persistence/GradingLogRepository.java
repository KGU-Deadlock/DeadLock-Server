package com.deadlock.hellocs.quiz.grading.adapter.out.persistence;

import com.deadlock.hellocs.quiz.grading.domain.GradingLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GradingLogRepository extends MongoRepository<GradingLog, String> {
}
