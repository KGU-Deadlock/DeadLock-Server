package com.deadlock.hellocs.user.application.port.out;

import com.deadlock.hellocs.user.domain.User;

public interface LoadUserPort {
    User loadUserByKakaoId(Long kakaoId);
    User loadUserByNickname(String nickname);
    boolean existsByNickname(String nickname);
}
