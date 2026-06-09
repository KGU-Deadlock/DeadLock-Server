package com.deadlock.hellocs.user.application.port.out;

import com.deadlock.hellocs.user.domain.User;

import java.util.List;

public interface LoadUserPort {
    User loadUserByKakaoId(Long kakaoId);
    List<User> loadUsersByKakaoIds(List<Long> kakaoIds);
    boolean existsByNickname(String nickname);
    boolean existsByKakaoId(Long kakaoId);
}
