package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

/**
 * 로그인 사용자에게 제공하는 상세 랭킹 응답.
 *
 * <p>상위 N명의 랭킹과 더불어, 사용자 자신의 순위 및
 * 바로 아래 몇 명(주로 2명)의 랭킹을 함께 보여주어
 * 다음 순위까지의 거리를 체감할 수 있게 함.</p>
 *
 * @param rankings       상위 N명의 랭킹 목록
 * @param myRank         로그인 사용자의 순위 (점수가 없으면 null)
 * @param nearbyRankings 내 바로 아래 최대 2명의 랭킹
 */
public record RankingDetailResult(
        List<RankingEntryResult> rankings,
        RankingEntryResult myRank,
        List<RankingEntryResult> nearbyRankings
) {}
