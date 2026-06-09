package com.deadlock.hellocs.user.application.port.in;

import com.deadlock.hellocs.user.application.port.in.dto.ProfileResult;
import com.deadlock.hellocs.user.application.port.in.dto.UserProfileSummaryResult;
import com.deadlock.hellocs.user.domain.Role;
import com.deadlock.hellocs.user.domain.UserLevel;

import java.util.List;

public interface LoadUserUseCase {
    ProfileResult getProfile(Long kakaoId);
    UserLevel getUserLevel(Long kakaoId);
    boolean isExist(Long kakaoId);
    Role getUserRole(Long kakaoId);
    List<Long> getInterestTopicIds(Long kakaoId);
    /** 랭킹 서비스용 프로필 조회 — 내부망 REST API로 제공됩니다. */
    UserProfileSummaryResult getProfileSummary(Long kakaoId);
    /** 랭킹 서비스용 벌크 프로필 조회 — 내부망 REST API로 제공됩니다. */
    List<UserProfileSummaryResult> getProfileSummaries(List<Long> kakaoIds);
}
