package com.deadlock.hellocs.user.application.port.in;

import java.util.List;

public interface LoadUserSummaryUseCase {
    UserSummaryResult getUserSummary(Long kakaoId);
    List<UserSummaryResult> getUserSummaries(List<Long> kakaoIds);
}
