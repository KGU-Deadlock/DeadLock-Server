package com.deadlock.hellocs.user.adapter.out.persistence;

import com.deadlock.hellocs.user.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserJpaEntity,Long> {
    Optional<UserJpaEntity> findByNickname(String nickname);
    Optional<UserJpaEntity> findTopByKakaoIdOrderByIdDesc(Long kakaoId);
    List<UserJpaEntity> findByKakaoIdIn(List<Long> kakaoIds);
    boolean existsByNickname(String nickname);
    boolean existsByKakaoId(Long kakaoId);
}
