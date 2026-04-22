package com.deadlock.hellocs.ranking.application.port.in;

import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingQueryType;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;

/**
 * 랭킹 조회(Query) 인바운드 포트.
 *
 * <p>헥사고날 아키텍처의 Inbound Port로, 외부(웹 컨트롤러 등)에서
 * 랭킹 데이터를 읽어갈 때 사용하는 유스케이스 계약이다.</p>
 */
public interface QueryRankingUseCase {

    /**
     * 비인증 사용자를 위한 랭킹 요약 정보를 조회함.
     * 상위 N명의 랭킹 + 전체 참가자 수를 반환함.
     */
    RankingSummaryResult getSummary(int size);

    /**
     * 로그인 사용자 기준의 상세 랭킹을 조회함.
     * 조회 기준(전체 / 관심 주제)에 따라 다른 ZSet을 참조함.
     */
    RankingDetailResult getRankingByType(Long userId, RankingQueryType type, int size);
}
