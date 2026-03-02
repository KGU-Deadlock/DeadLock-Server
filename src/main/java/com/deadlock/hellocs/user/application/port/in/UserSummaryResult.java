package com.deadlock.hellocs.user.application.port.in;

public record UserSummaryResult(
        Long kakaoId,
        String nickname,
        String profileImage
) {
}
