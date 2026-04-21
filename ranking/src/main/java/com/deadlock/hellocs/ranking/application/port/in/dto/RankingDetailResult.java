package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

public record RankingDetailResult(
        List<RankingEntryResult> rankings,
        RankingEntryResult myRank,
        List<RankingEntryResult> nearbyRankings
) {}
