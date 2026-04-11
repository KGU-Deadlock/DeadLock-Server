package com.deadlock.hellocs.ranking.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MyRankingResult(
        @Schema(description = "현재 로그인한 사용자의 카카오 식별자입니다.", example = "10001")
        Long kakaoId,
        @Schema(description = "현재 로그인한 사용자의 닉네임입니다.", example = "cs_runner")
        String nickname,
        @Schema(description = "현재 로그인한 사용자의 프로필 이미지 URL입니다.", example = "https://cdn.example.com/profiles/10001.png", nullable = true)
        String profileImage,
        @Schema(description = "현재 로그인한 사용자의 관심 주제 목록입니다.")
        List<String> interests,
        @Schema(description = "랭킹 순위입니다. 아직 랭킹에 반영되지 않은 경우 null입니다.", example = "12", nullable = true)
        Long rank,
        @Schema(description = "누적 점수입니다.", example = "840")
        long score
) {
}
