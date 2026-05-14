package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

public record RankingSummaryResult(
        List<RankingEntryResult> top5,
        int recentRelatedDiscussionCount
) {}
