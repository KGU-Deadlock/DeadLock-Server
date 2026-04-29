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

/**
 * Redis ZSet 기반 랭킹 영속성 어댑터.
 *
 * <p>Redis의 Sorted Set(ZSet)을 활용하여 "점수 기준 자동 정렬" 및 "순위 조회 O(log N)"을 그대로 이용함.
 * ZSet의 member는 사용자 ID 문자열, score는 누적 점수로 저장된다.</p>
 *
 * <h3>순위 보정</h3>
 * <p>Redis의 {@code reverseRank}는 0-based로 반환되므로, 서비스 레이어에서 사용하는
 * 1-based 순위로 변환하기 위해 {@code +1} 보정을 함. 반대로 범위 조회 시에는
 * {@code startRank - 1}, {@code endRank - 1}로 오프셋을 맞춘다.</p>
 */
@Component
@RequiredArgsConstructor
public class RankingPersistenceAdapter implements SaveRankingPort, LoadRankingPort {

    private final StringRedisTemplate redisTemplate;

    /**
     * ZSet의 {@code ZINCRBY} 연산을 이용해 기존 점수에 가산함.
     * member가 없으면 새로 생성되고, 있으면 기존 점수에 더해진다.
     */
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

    /**
     * 전체 랭킹 보드에 등록된 참가자 수.
     * Redis의 ZCARD에 해당함. 값이 null이면 0을 반환함.
     */
    @Override
    public long countTotal() {
        Long count = redisTemplate.opsForZSet().size(RankingKey.total().redisKey());
        return count != null ? count : 0L;
    }

    /**
     * 특정 사용자의 순위/점수 조회.
     *
     * <p>reverseRank: 점수 내림차순 기준 0-based 순위. 등록되지 않은 사용자이면 {@code null}.
     * score: 해당 member의 점수. 둘 중 하나라도 null이면 "랭킹에 없음"으로 간주하여 {@code null}을 반환함.
     * 조회된 값은 1-based로 보정하여 도메인 객체로 래핑함.</p>
     */
    private Ranking loadUserRankFromKey(String key, Long userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId.toString());
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        if (rank == null || score == null) return null;
        return new Ranking(userId, score.longValue(), rank + 1);
    }

    /**
     * 순위 범위로 랭킹을 조회함.
     *
     * <p>서비스에서 넘겨받는 startRank/endRank는 1-based inclusive이지만
     * Redis는 0-based inclusive이므로 각각 -1 처리함.
     * 조회 결과 tuples는 이미 점수 내림차순으로 정렬되어 있으므로
     * {@code startRank}부터 순차적으로 rank를 증가시키며 도메인 객체로 매핑함.</p>
     */
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
