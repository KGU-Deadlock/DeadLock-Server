package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

public record RankingDetailResult(
        String filterType,
        List<RankingEntryResult> rankings,
        MyRankingResult myRank,
        List<RankingEntryResult> nearbyRankings,
        int recentRelatedDiscussionCount
) {}
