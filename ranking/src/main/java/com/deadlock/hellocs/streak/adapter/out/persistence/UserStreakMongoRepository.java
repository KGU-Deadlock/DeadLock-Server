package com.deadlock.hellocs.streak.adapter.out.persistence;

import com.deadlock.hellocs.streak.adapter.out.persistence.entity.UserStreakMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserStreakMongoRepository extends MongoRepository<UserStreakMongoEntity, String> {

    Optional<UserStreakMongoEntity> findByUserId(Long userId);
}
