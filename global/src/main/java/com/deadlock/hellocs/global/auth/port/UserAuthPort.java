package com.deadlock.hellocs.global.auth.port;

public interface UserAuthPort {
    boolean isExist(Long kakaoId);
    String getUserRoleName(Long kakaoId);
}
