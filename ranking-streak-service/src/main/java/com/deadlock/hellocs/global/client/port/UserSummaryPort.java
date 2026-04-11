package com.deadlock.hellocs.global.client.port;

import com.deadlock.hellocs.global.client.dto.UserSummaryResult;

import java.util.List;

/**
 * core-service의 LoadUserSummaryUseCase를 대체하는 로컬 포트.
 * 구현체는 CoreServiceUserAdapter (HTTP 호출).
 */
public interface UserSummaryPort {
    UserSummaryResult getUserSummary(Long kakaoId);
    List<UserSummaryResult> getUserSummaries(List<Long> kakaoIds);
}
