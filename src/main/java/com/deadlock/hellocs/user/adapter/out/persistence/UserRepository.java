package com.deadlock.hellocs.user.adapter.out.persistence;

import com.deadlock.hellocs.user.adapter.out.persistence.entity.UserJpaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserJpaJpaEntity,Long> {
    Optional<UserJpaJpaEntity> findByNickname(String nickname);
    Optional<UserJpaJpaEntity> findByKakaoId(Long kakaoId);
}
