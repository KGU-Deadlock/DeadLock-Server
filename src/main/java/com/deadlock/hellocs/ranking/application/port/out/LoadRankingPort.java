package com.deadlock.hellocs.ranking.application.port.out;

import com.deadlock.hellocs.ranking.domain.RankingEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadRankingPort {
    List<RankingEntry> loadTopRankings(int size);
    Optional<RankingEntry> loadRanking(Long kakaoId);
    // 수정 표시
    List<RankingEntry> loadRankingsByRankRange(long startRank, long endRank);
    // 수정 표시
    Map<Long, Long> loadTopicScores(Long topicId);
}
