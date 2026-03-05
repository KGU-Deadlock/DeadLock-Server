package com.deadlock.hellocs.ranking.adapter.out.redis;

import com.deadlock.hellocs.ranking.application.port.out.LoadRankingPort;
import com.deadlock.hellocs.ranking.application.port.out.UpdateRankingPort;
import com.deadlock.hellocs.ranking.domain.RankingEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RankingRedisAdapter implements LoadRankingPort, UpdateRankingPort {

    // 수정 표시
    private static final String GLOBAL_RANKING_KEY = "ranking:global";
    // 수정 표시
    private static final String TOPIC_RANKING_KEY_PREFIX = "ranking:topic:";
    // 수정 표시
    private static final String IDEMPOTENCY_KEY_PREFIX = "ranking:applied:";
    private static final RedisScript<Long> INCREMENT_SCRIPT = RedisScript.of("""
            if redis.call('SETNX', KEYS[1], '1') == 0 then
                return 0
            end
            redis.call('EXPIRE', KEYS[1], 86400)
            if tonumber(ARGV[1]) > 0 then
                for i = 2, #KEYS do
                    redis.call('ZINCRBY', KEYS[i], ARGV[1], ARGV[2])
                end
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean increaseScoreIfAbsent(String gradingLogId, Long kakaoId, int score, List<Long> topicIds) {
        // 수정 표시
        List<String> keys = new ArrayList<>();
        keys.add(idempotencyKey(gradingLogId));
        keys.add(GLOBAL_RANKING_KEY);
        topicIds.stream()
                .distinct()
                .map(this::topicRankingKey)
                .forEach(keys::add);

        Long result = stringRedisTemplate.execute(
                INCREMENT_SCRIPT,
                keys,
                String.valueOf(score),
                String.valueOf(kakaoId)
        );
        return result != null && result == 1L;
    }

    @Override
    public List<RankingEntry> loadTopRankings(int size) {
        return loadRankingsByRange(GLOBAL_RANKING_KEY, 1L, size);
    }

    @Override
    public Optional<RankingEntry> loadRanking(Long kakaoId) {
        Long rank = stringRedisTemplate.opsForZSet().reverseRank(GLOBAL_RANKING_KEY, String.valueOf(kakaoId));
        Double score = stringRedisTemplate.opsForZSet().score(GLOBAL_RANKING_KEY, String.valueOf(kakaoId));

        if (rank == null || score == null) {
            return Optional.empty();
        }

        return Optional.of(new RankingEntry(kakaoId, score.longValue(), rank + 1L));
    }

    @Override
    public List<RankingEntry> loadRankingsByRankRange(long startRank, long endRank) {
        // 수정 표시
        if (startRank > endRank) {
            return List.of();
        }
        return loadRankingsByRange(GLOBAL_RANKING_KEY, startRank, endRank);
    }

    @Override
    public Map<Long, Long> loadTopicScores(Long topicId) {
        // 수정 표시
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(topicRankingKey(topicId), 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> scores = new HashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            scores.put(Long.valueOf(tuple.getValue()), tuple.getScore().longValue());
        }
        return scores;
    }

    private List<RankingEntry> loadRankingsByRange(String key, long startRank, long endRank) {
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(key, startRank - 1L, endRank - 1L);

        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        List<RankingEntry> rankings = new ArrayList<>(tuples.size());
        long rank = startRank;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            rankings.add(new RankingEntry(Long.valueOf(tuple.getValue()), tuple.getScore().longValue(), rank++));
        }
        return rankings;
    }

    private String idempotencyKey(String gradingLogId) {
        return IDEMPOTENCY_KEY_PREFIX + gradingLogId;
    }

    private String topicRankingKey(Long topicId) {
        return TOPIC_RANKING_KEY_PREFIX + topicId;
    }
}
