package com.deadlock.hellocs.user.application.port.in.dto;

import java.util.List;

/**
 * 랭킹 서비스가 필요로 하는 사용자 프로필 요약.
 * {@code GET /v1/users/{userId}/profile-summary} 와
 * {@code GET /v1/users/profile-summaries} 엔드포인트를 통해 노출됩니다.
 */
public record UserProfileSummaryResult(
        Long userId,
        String nickname,
        String profileImage,
        List<String> interests
) {
}
