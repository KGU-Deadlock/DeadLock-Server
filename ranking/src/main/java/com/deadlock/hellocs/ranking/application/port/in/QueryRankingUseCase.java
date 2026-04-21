package com.deadlock.hellocs.ranking.application.port.in;

import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingQueryType;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;

public interface QueryRankingUseCase {

    RankingSummaryResult getSummary(int size);

    RankingDetailResult getRankingByType(Long userId, RankingQueryType type, int size);
}
