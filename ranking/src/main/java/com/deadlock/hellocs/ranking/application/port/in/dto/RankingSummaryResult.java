package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

public record RankingSummaryResult(
        List<RankingEntryResult> topEntries,
        long totalCount
) {}
