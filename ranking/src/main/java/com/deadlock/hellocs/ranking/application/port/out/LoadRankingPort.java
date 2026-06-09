package com.deadlock.hellocs.ranking.application.port.out;

import com.deadlock.hellocs.ranking.domain.Ranking;
import com.deadlock.hellocs.ranking.domain.RankingKey;

import java.util.List;

/**
 * 랭킹 데이터를 읽어오는 아웃바운드 포트.
 *
 * <p>현재 구현은 Redis ZSet({@code RankingPersistenceAdapter})이며,
 * 서비스는 이 인터페이스만 의존하므로 저장소 교체 시 서비스 코드는 변경되지 않는다.</p>
 */
public interface LoadRankingPort {

    /**
     * 특정 보드({@link RankingKey})에서 지정한 사용자의 순위/점수를 조회함.
     * 점수가 등록되어 있지 않으면 {@code null}을 반환함.
     */
    Ranking loadUserRank(RankingKey key, Long userId);

    /**
     * 순위 범위를 지정하여 랭킹 목록을 조회함. (1-based, inclusive)
     * 예: startRank=1, endRank=5 → 1위부터 5위까지.
     */
    List<Ranking> loadByRankRange(RankingKey key, long startRank, long endRank);

    /** 전체 랭킹 보드에 등록된 참가자 수를 반환함. */
    long countTotal();
}
