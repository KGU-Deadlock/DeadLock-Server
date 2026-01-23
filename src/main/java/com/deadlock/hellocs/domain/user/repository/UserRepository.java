package com.deadlock.hellocs.domain.user.repository;

import com.deadlock.hellocs.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    User findByNickname(String nickname);
    Optional<User> findByKakaoId(Long kakaoId);
}
