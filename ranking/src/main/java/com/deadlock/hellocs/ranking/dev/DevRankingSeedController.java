package com.deadlock.hellocs.ranking.dev;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.ranking.domain.RankingKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/dev")
public class DevRankingSeedController {

    private final StringRedisTemplate redisTemplate;

    @PostMapping("/ranking")
    public ApiResponse<SeedRankingResult> seedRanking(@RequestBody SeedRequest request) {

        List<UserSpec> specs = request.users();

        Map<Long, Long>            totalScores = new HashMap<>(specs.size());
        Map<Long, Map<Long, Long>> topicScores = new HashMap<>();

        for (UserSpec spec : specs) {
            long  kakaoId    = spec.kakaoId();
            int   accountAge = spec.accountAgeDays();
            Random rng       = new Random(request.seed() + kakaoId * 10_000L);

            long userTotal = 0L;

            for (int d = accountAge - 1; d >= 0; d--) {
                if ((d % 7) >= spec.dpw()) continue;

                long topicId = (long)((int)(kakaoId + d) % request.numTopics()) + 1L;

                int sessionScore = 0;
                for (int i = 0; i < request.quizPerDay(); i++) {
                    boolean isCorrect = rng.nextInt(3) != 0;
                    sessionScore += isCorrect ? (10 + rng.nextInt(6)) : 0;
                }

                userTotal += sessionScore;
                topicScores.computeIfAbsent(topicId, k -> new HashMap<>())
                        .merge(kakaoId, (long) sessionScore, Long::sum);
            }

            if (userTotal > 0) {
                totalScores.put(kakaoId, userTotal);
            }
        }

        // 기존 키 삭제 (멱등성)
        Set<String> keysToDelete = new HashSet<>();
        keysToDelete.add(RankingKey.total().redisKey());
        for (Long topicId : topicScores.keySet()) {
            keysToDelete.add(RankingKey.topic(topicId).redisKey());
        }
        redisTemplate.delete(keysToDelete);

        batchZAdd(RankingKey.total().redisKey(), totalScores);
        log.info("[DevRankingSeedController] ranking:total ZADD 완료: {} 멤버", totalScores.size());

        for (Map.Entry<Long, Map<Long, Long>> entry : topicScores.entrySet()) {
            batchZAdd(RankingKey.topic(entry.getKey()).redisKey(), entry.getValue());
        }
        log.info("[DevRankingSeedController] ranking:topic:* ZADD 완료: {} 토픽", topicScores.size());

        return ApiResponse.onSuccess(new SeedRankingResult(totalScores.size(), topicScores.size()));
    }

    private void batchZAdd(String key, Map<Long, Long> scores) {
        if (scores.isEmpty()) return;
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(scores.size());
        for (Map.Entry<Long, Long> e : scores.entrySet()) {
            tuples.add(new DefaultTypedTuple<>(e.getKey().toString(), e.getValue().doubleValue()));
        }
        redisTemplate.opsForZSet().add(key, tuples);
    }

    // ─── 요청/결과 레코드 ────────────────────────────────────────────────────

    record UserSpec(long kakaoId, int dpw, int accountAgeDays) {}
    record SeedRequest(List<UserSpec> users, int quizPerDay, int numTopics, long seed) {}

    public record SeedRankingResult(int totalMembers, int topicKeysCreated) {}
}
