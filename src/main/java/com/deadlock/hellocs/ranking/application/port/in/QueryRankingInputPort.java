package com.deadlock.hellocs.ranking.application.port.in;

import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;

public interface QueryRankingInputPort {
    // 수정 표시
    RankingSummaryResult getSummary();
    // 수정 표시
    RankingDetailResult getRanking(Long kakaoId, String filterType, int size);
}
