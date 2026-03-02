package com.deadlock.hellocs.ranking.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RankingEntryResult(
        // Swagger 추가
        @Schema(description = "전역 랭킹 순위입니다.", example = "1")
        long rank,
        // Swagger 추가
        @Schema(description = "사용자의 카카오 식별자입니다.", example = "10001")
        Long kakaoId,
        // Swagger 추가
        @Schema(description = "랭킹에 노출되는 사용자 닉네임입니다.", example = "cs_runner")
        String nickname,
        // Swagger 추가
        @Schema(description = "사용자 프로필 이미지 URL입니다.", example = "https://cdn.example.com/profiles/10001.png", nullable = true)
        String profileImage,
        // Swagger 추가
        @Schema(description = "퀴즈 채점 완료 시 누적 반영된 총 점수입니다.", example = "1280")
        long score
) {
}
