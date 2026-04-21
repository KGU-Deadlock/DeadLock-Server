package com.deadlock.hellocs.ranking.adapter.out.persistence;

import com.deadlock.hellocs.ranking.application.port.out.LoadRankingPort;
import com.deadlock.hellocs.ranking.application.port.out.SaveRankingPort;
import com.deadlock.hellocs.ranking.domain.Ranking;
import com.deadlock.hellocs.ranking.domain.RankingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RankingPersistenceAdapter implements SaveRankingPort, LoadRankingPort {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void incrementScore(RankingKey key, Long userId, int score) {
        redisTemplate.opsForZSet().incrementScore(key.redisKey(), userId.toString(), score);
    }

    @Override
    public Ranking loadUserRank(RankingKey key, Long userId) {
        return loadUserRankFromKey(key.redisKey(), userId);
    }

    @Override
    public List<Ranking> loadByRankRange(RankingKey key, long startRank, long endRank) {
        return loadRangeFromKey(key.redisKey(), startRank, endRank);
    }

    @Override
    public long countTotal() {
        Long count = redisTemplate.opsForZSet().size(RankingKey.total().redisKey());
        return count != null ? count : 0L;
    }

    private Ranking loadUserRankFromKey(String key, Long userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId.toString());
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        if (rank == null || score == null) return null;
        return new Ranking(userId, score.longValue(), rank + 1);
    }

    private List<Ranking> loadRangeFromKey(String key, long startRank, long endRank) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, startRank - 1, endRank - 1);
        if (tuples == null) return List.of();

        List<Ranking> result = new ArrayList<>();
        long rank = startRank;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            result.add(new Ranking(Long.valueOf(t.getValue()), t.getScore().longValue(), rank++));
        }
        return result;
    }
}
