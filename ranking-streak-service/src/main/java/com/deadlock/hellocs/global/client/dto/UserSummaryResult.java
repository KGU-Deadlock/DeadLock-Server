package com.deadlock.hellocs.global.client.dto;

public record UserSummaryResult(
        Long kakaoId,
        String nickname,
        String profileImage
) {}
