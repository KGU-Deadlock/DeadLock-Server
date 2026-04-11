package com.deadlock.hellocs.global.client.port;

import com.deadlock.hellocs.global.client.dto.UserDetail;

import java.util.List;

/**
 * core-service의 LoadUserPort 중 ranking에 필요한 메서드만 정의한 로컬 포트.
 */
public interface UserDetailPort {
    UserDetail getUserDetail(Long kakaoId);
    List<UserDetail> getUserDetails(List<Long> kakaoIds);
}
