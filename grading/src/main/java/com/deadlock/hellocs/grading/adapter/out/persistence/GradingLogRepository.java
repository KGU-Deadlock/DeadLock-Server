package com.deadlock.hellocs.grading.adapter.out.persistence;

import com.deadlock.hellocs.grading.adapter.out.persistence.entity.GradingLogMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GradingLogRepository extends MongoRepository<GradingLogMongoEntity, String> {
    List<GradingLogMongoEntity> findAllByUserId(Long userId);

    /** DEV-SEED id 패턴으로 시작하는 로그 존재 여부 확인 (멱등성 체크용). */
    boolean existsByIdStartingWith(String prefix);
}
