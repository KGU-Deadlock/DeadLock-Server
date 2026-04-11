package com.deadlock.hellocs.ranking.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RankingSummaryResult(
        // 수정 표시
        @Schema(description = "전역 랭킹 상위 5명 목록입니다.")
        List<RankingEntryResult> top5,
        // 수정 표시
        @Schema(description = "최근 관련 논의 수입니다. 현재 구현에서는 항상 0을 반환합니다.", example = "0")
        int recentRelatedDiscussionCount
) {
}
