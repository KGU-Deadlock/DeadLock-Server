package com.deadlock.hellocs.streak.adapter.out.persistence;

import com.deadlock.hellocs.streak.adapter.out.persistence.entity.UserStreakMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** 사용자 스트릭의 MongoDB 레포지토리. userId로 단건 조회를 지원함. */
public interface UserStreakMongoRepository extends MongoRepository<UserStreakMongoEntity, String> {

    Optional<UserStreakMongoEntity> findByUserId(Long userId);
}
