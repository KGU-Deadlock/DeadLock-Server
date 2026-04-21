package com.deadlock.hellocs.ranking.application.port.out;

import com.deadlock.hellocs.ranking.domain.RankingKey;

public interface SaveRankingPort {

    void incrementScore(RankingKey key, Long userId, int score);
}
