package com.deadlock.hellocs.ranking.application.port.out;

import com.deadlock.hellocs.ranking.domain.Ranking;
import com.deadlock.hellocs.ranking.domain.RankingKey;

import java.util.List;

public interface LoadRankingPort {

    Ranking loadUserRank(RankingKey key, Long userId);

    List<Ranking> loadByRankRange(RankingKey key, long startRank, long endRank);

    long countTotal();
}
