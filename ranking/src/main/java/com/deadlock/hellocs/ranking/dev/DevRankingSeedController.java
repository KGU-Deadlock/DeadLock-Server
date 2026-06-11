package com.deadlock.hellocs.ranking.dev;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * dev 서비스가 호출하는 랭킹 시딩 엔드포인트 (세그먼트 분포 모델).
 *
 * <p>DevGradingSeedController와 동일한 결정론적 활동 캘린더 및 점수 계산으로
 * Redis ZSet(ranking:total, ranking:topic:{id})에 누적 점수를 batch ZADD한다.
 * 이벤트를 경유하지 않으므로 빠르게 10k 유저의 랭킹을 구성할 수 있다.</p>
 *
 * <p>기존 ranking:* 키는 DEL 후 재구성한다(멱등성).</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/dev")
public class DevRankingSeedController {

    private static final String RANKING_TOTAL_KEY  = "ranking:total";
    private static final String RANKING_TOPIC_KEY  = "ranking:topic:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 랭킹 시딩.
     * grading/streak 시드와 동일한 파라미터를 사용해 동일한 점수를 재현한다.
     */
    @PostMapping("/ranking")
    public ApiResponse<SeedRankingResult> seedRanking(
            @RequestParam(name = "users",            defaultValue = "100")  int   users,
            @RequestParam(name = "signupWindowDays", defaultValue = "180")  int   signupWindowDays,
            @RequestParam(name = "quizPerDay",       defaultValue = "30")   int   quizPerDay,
            @RequestParam(name = "numTopics",        defaultValue = "6")    int   numTopics,
            @RequestParam(name = "segPowerShare",    defaultValue = "0.2")  float segPowerShare,
            @RequestParam(name = "segRegularShare",  defaultValue = "0.5")  float segRegularShare,
            @RequestParam(name = "segPowerDpw",      defaultValue = "7")    int   segPowerDpw,
            @RequestParam(name = "segRegularDpw",    defaultValue = "4")    int   segRegularDpw,
            @RequestParam(name = "segCasualDpw",     defaultValue = "2")    int   segCasualDpw,
            @RequestParam(name = "tokenPoolSize",    defaultValue = "1000") int   tokenPoolSize,
            @RequestParam(name = "seed",             defaultValue = "42")   long  seed) {

        SegmentLayout layout = new SegmentLayout(users, tokenPoolSize, segPowerShare, segRegularShare);
        LocalDate today = LocalDate.now();

        // 유저별 총점 + 토픽별 점수 누적 (메모리 내 집계 후 일괄 ZADD)
        Map<Long, Long>              totalScores = new HashMap<>(users);
        Map<Long, Map<Long, Long>>   topicScores = new HashMap<>();

        for (int idx = 0; idx < users; idx++) {
            long kakaoId    = 1001L + idx;
            int  dpw        = layout.dpwOf(idx, segPowerDpw, segRegularDpw, segCasualDpw);
            int  accountAge = layout.accountAgeDays(idx, signupWindowDays);
            Random rng      = new Random(seed + kakaoId * 10_000L);

            long userTotal = 0L;

            for (int d = accountAge - 1; d >= 0; d--) {
                if ((d % 7) >= dpw) continue;

                long topicId = (long)((int)(kakaoId + d) % numTopics) + 1L;

                // grading seeder와 동일한 RNG 시퀀스로 세션 점수 재현
                int sessionScore = 0;
                for (int i = 0; i < quizPerDay; i++) {
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
        keysToDelete.add(RANKING_TOTAL_KEY);
        for (Long topicId : topicScores.keySet()) {
            keysToDelete.add(RANKING_TOPIC_KEY + topicId);
        }
        redisTemplate.delete(keysToDelete);

        // ranking:total batch ZADD
        batchZAdd(RANKING_TOTAL_KEY, totalScores);
        log.info("[DevRankingSeedController] ranking:total ZADD 완료: {} 멤버", totalScores.size());

        // ranking:topic:{id} batch ZADD
        for (Map.Entry<Long, Map<Long, Long>> entry : topicScores.entrySet()) {
            String key = RANKING_TOPIC_KEY + entry.getKey();
            batchZAdd(key, entry.getValue());
        }
        log.info("[DevRankingSeedController] ranking:topic:* ZADD 완료: {} 토픽", topicScores.size());

        return ApiResponse.onSuccess(new SeedRankingResult(
                totalScores.size(), topicScores.size()));
    }

    private void batchZAdd(String key, Map<Long, Long> scores) {
        if (scores.isEmpty()) return;
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(scores.size());
        for (Map.Entry<Long, Long> e : scores.entrySet()) {
            tuples.add(new DefaultTypedTuple<>(e.getKey().toString(), e.getValue().doubleValue()));
        }
        redisTemplate.opsForZSet().add(key, tuples);
    }

    // ─── 세그먼트 레이아웃 헬퍼 (DevGradingSeedController와 동일 알고리즘) ──────

    static final class SegmentLayout {

        private final int users, tokenPoolSize;
        private final int poolPower, poolRegular, poolCasual;
        private final int totalPower, totalRegular;
        private final int bgPower, bgRegular;

        SegmentLayout(int users, int tokenPoolSize, float powerShare, float regularShare) {
            this.users         = users;
            this.tokenPoolSize = Math.min(tokenPoolSize, users);
            this.poolPower     = (int) (this.tokenPoolSize * powerShare);
            this.poolRegular   = (int) (this.tokenPoolSize * regularShare);
            this.poolCasual    = this.tokenPoolSize - this.poolPower - this.poolRegular;
            this.totalPower    = (int) (users * powerShare);
            this.totalRegular  = (int) (users * regularShare);
            this.bgPower       = Math.max(0, totalPower   - poolPower);
            this.bgRegular     = Math.max(0, totalRegular - poolRegular);
        }

        int dpwOf(int idx, int powerDpw, int regularDpw, int casualDpw) {
            return switch (segmentOf(idx)) {
                case POWER   -> powerDpw;
                case REGULAR -> regularDpw;
                case CASUAL  -> casualDpw;
            };
        }

        int accountAgeDays(int idx, int signupWindowDays) {
            int[] info    = segmentIdxAndSize(idx);
            int segIdx    = info[0];
            int segSize   = info[1];
            if (segSize <= 1) return signupWindowDays;
            return (segSize - 1 - segIdx) * signupWindowDays / (segSize - 1);
        }

        private Segment segmentOf(int idx) {
            if (idx < poolPower)                   return Segment.POWER;
            if (idx < poolPower + poolRegular)     return Segment.REGULAR;
            if (idx < tokenPoolSize)               return Segment.CASUAL;
            int bgIdx = idx - tokenPoolSize;
            if (bgIdx < bgPower)                   return Segment.POWER;
            if (bgIdx < bgPower + bgRegular)       return Segment.REGULAR;
            return Segment.CASUAL;
        }

        private int[] segmentIdxAndSize(int idx) {
            if (idx < poolPower)
                return new int[]{ idx, poolPower };
            if (idx < poolPower + poolRegular)
                return new int[]{ idx - poolPower, poolRegular };
            if (idx < tokenPoolSize)
                return new int[]{ idx - poolPower - poolRegular, poolCasual };
            int bgIdx = idx - tokenPoolSize;
            if (bgIdx < bgPower)
                return new int[]{ bgIdx, bgPower };
            if (bgIdx < bgPower + bgRegular)
                return new int[]{ bgIdx - bgPower, bgRegular };
            int bgCasual = users - tokenPoolSize - bgPower - bgRegular;
            return new int[]{ bgIdx - bgPower - bgRegular, Math.max(1, bgCasual) };
        }

        enum Segment { POWER, REGULAR, CASUAL }
    }

    // ─── 결과 레코드 ─────────────────────────────────────────────────────────

    public record SeedRankingResult(int totalMembers, int topicKeysCreated) {}
}
