package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

/**
 * 비인증 화면에서 노출되는 랭킹 요약 응답.
 *
 * @param topEntries 상위 랭커 목록 (예: 상위 5명)
 * @param totalCount 랭킹에 점수가 등록된 전체 참가자 수
 */
public record RankingSummaryResult(
        List<RankingEntryResult> topEntries,
        long totalCount
) {}
