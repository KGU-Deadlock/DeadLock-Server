package com.deadlock.hellocs.ranking.application.port.in;

import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public interface QueryRankingInputPort {
    // 수정 표시
    RankingSummaryResult getSummary();
    // 수정 표시
    RankingDetailResult getRanking(
            @NotNull Long kakaoId,
            @NotNull String filterType,
            @Min(10) @Max(100) int size
    );
}
