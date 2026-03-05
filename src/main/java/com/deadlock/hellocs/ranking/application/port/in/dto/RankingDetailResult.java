package com.deadlock.hellocs.ranking.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RankingDetailResult(
        // 수정 표시
        @Schema(description = "랭킹 필터 유형입니다.", example = "ALL")
        String filterType,
        // 수정 표시
        @Schema(description = "조건에 맞는 랭킹 목록입니다.")
        List<RankingEntryResult> rankings,
        // 수정 표시
        @Schema(description = "현재 로그인한 사용자의 랭킹 정보입니다.")
        MyRankingResult myRanking,
        // 수정 표시
        @Schema(description = "내 순위 바로 아래 2개의 랭킹 정보입니다.")
        List<RankingEntryResult> belowMyRankings,
        // 수정 표시
        @Schema(description = "최근 관련 논의 수입니다. 현재 구현에서는 항상 0을 반환합니다.", example = "0")
        int recentRelatedDiscussionCount
) {
}
