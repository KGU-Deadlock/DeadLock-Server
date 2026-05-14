package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

public record RankingDetailResult(
        String filterType,
        List<RankingEntryResult> rankings,
        MyRankingResult myRanking,
        List<RankingEntryResult> belowMyRankings,
        int recentRelatedDiscussionCount
) {}
