package com.deadlock.hellocs.ranking.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RankingEntryResult(
        @Schema(description = "랭킹 순위입니다.", example = "1")
        long rank,
        @Schema(description = "사용자의 카카오 고유 ID입니다.", example = "10001")
        Long kakaoId,
        @Schema(description = "랭킹에 노출되는 사용자의 닉네임입니다.", example = "cs_runner")
        String nickname,
        @Schema(description = "사용자 프로필 이미지 URL입니다.", example = "https://cdn.example.com/profiles/10001.png", nullable = true)
        String profileImage,
        @Schema(description = "사용자의 관심 토픽 목록입니다.", example = "[\"OS\", \"Network\"]")
        List<String> interests,
        @Schema(description = "누적 반영된 총 점수입니다.", example = "1280")
        long score
) {
}
