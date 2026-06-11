package com.deadlock.hellocs.grading.dev;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.grading.adapter.out.persistence.entity.GradingLogMongoEntity;
import com.deadlock.hellocs.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import com.deadlock.hellocs.quiz.contract.QuizMode;
import com.deadlock.hellocs.quiz.contract.QuizType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * dev 서비스가 호출하는 채점 기록 시딩 엔드포인트 (세그먼트 분포 모델).
 *
 * <p>dataset.env 파라미터를 그대로 받아 power/regular/casual 세그먼트 분포 기반으로
 * grading_logs(MongoDB)에 DEV-SEED 채점 기록을 bulk insert한다.
 * 파생 데이터(streak/ranking)는 각 서비스의 전용 시드 엔드포인트가 직접 생성하므로
 * 이 엔드포인트는 이벤트를 발행하지 않는다.</p>
 *
 * <h3>세그먼트 레이아웃 (kakaoId 순서)</h3>
 * <pre>
 *   토큰 풀 영역 (idx 0..tokenPoolSize-1):
 *     power   블록: idx 0          .. poolPower-1
 *     regular 블록: idx poolPower  .. poolPower+poolRegular-1
 *     casual  블록: idx poolPower+poolRegular .. tokenPoolSize-1
 *   배경 영역 (idx tokenPoolSize..users-1):
 *     power/regular/casual 순 배치 (나머지 인원)
 *
 *   accountAge: 세그먼트 블록 내에서 첫 번째 유저(segIdx=0)가 signupWindowDays(180일, 最古),
 *               마지막 유저가 0일(신규)로 역순 배치 → 토큰 풀 사용자는 데이터가 풍부.
 * </pre>
 *
 * <h3>활동 캘린더</h3>
 * <pre>
 *   activeDays = accountAgeDays * dpw / 7  (연속 슬롯, 주당 dpw일 패턴)
 *   day d (0=오늘, accountAgeDays-1=가장 오래된 날): (d % 7) < dpw 이면 활동일
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/v1/internal/dev")
public class DevGradingSeedController {

    private static final String DEV_SEED_PREFIX      = "dev-seed-";
    private static final String QUIZ_ANSWER_KEY_PATH = "/v1/internal/dev/quiz-bank/answer-keys";
    private static final int    BATCH_SIZE           = 500; // 유저 단위 배치

    private final MongoTemplate  mongoTemplate;
    private final RestClient     quizClient;
    private final GradingProgress progress;

    public DevGradingSeedController(
            MongoTemplate mongoTemplate,
            GradingProgress progress,
            @Value("${service.quiz.url}") String quizServiceUrl) {
        this.mongoTemplate = mongoTemplate;
        this.progress      = progress;
        this.quizClient    = RestClient.create(quizServiceUrl);
    }

    @GetMapping("/grading-logs/progress")
    public ApiResponse<GradingProgress.Snapshot> getProgress() {
        return ApiResponse.onSuccess(progress.snapshot());
    }

    /**
     * 채점 기록 시딩 (세그먼트 분포 모델).
     *
     * @param users             전체 유저 수 (kakaoId 1001~1000+users)
     * @param signupWindowDays  계정나이 분산 범위 (일). 기본 180.
     * @param quizPerDay        활동일당 풀이 문항 수. 기본 30.
     * @param numTopics         토픽 수 (ID 1~numTopics). 기본 6.
     * @param segPowerShare     power 유저 비율. 기본 0.2.
     * @param segRegularShare   regular 유저 비율. 기본 0.5.
     * @param segPowerDpw       power 주당 활동일. 기본 7.
     * @param segRegularDpw     regular 주당 활동일. 기본 4.
     * @param segCasualDpw      casual 주당 활동일. 기본 2.
     * @param tokenPoolSize     k6 토큰 풀 크기. 기본 1000.
     * @param seed              결정론적 RNG 시드. 기본 42.
     */
    @PostMapping("/grading-logs")
    public ApiResponse<SeedGradingLogsResult> seedGradingLogs(
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

        List<QuizAnswerKey> answerKeys = fetchAnswerKeys();
        if (answerKeys.isEmpty()) {
            log.warn("[DevGradingSeedController] DEV-SEED 퀴즈 없음. quiz-bank 먼저 시딩하세요.");
            return ApiResponse.onSuccess(new SeedGradingLogsResult(0, 0));
        }

        progress.start(users);

        SegmentLayout layout = new SegmentLayout(
                users, tokenPoolSize, segPowerShare, segRegularShare);

        LocalDate today    = LocalDate.now();
        int totalDocs      = 0;
        int totalItems     = 0;
        List<GradingLogMongoEntity> batch = new ArrayList<>(BATCH_SIZE * 200);

        for (int idx = 0; idx < users; idx++) {
            long  kakaoId      = 1001L + idx;
            int   dpw          = layout.dpwOf(idx, segPowerDpw, segRegularDpw, segCasualDpw);
            int   accountAge   = layout.accountAgeDays(idx, signupWindowDays);
            Random rng         = new Random(seed + kakaoId * 10_000L);

            for (int d = accountAge - 1; d >= 0; d--) {
                if ((d % 7) >= dpw) continue;           // 비활동일 스킵

                LocalDate date     = today.minusDays(d);
                long sessionTopicId = (long)((int)(kakaoId + d) % numTopics) + 1L;

                LocalDateTime solvedAt = date.atTime(8 + rng.nextInt(14), rng.nextInt(60));
                List<GradingItem> items = buildItems(answerKeys, kakaoId, d, quizPerDay, rng);

                int correctCount = (int) items.stream().filter(GradingItem::isCorrect).count();
                int totalScore   = items.stream().mapToInt(GradingItem::score).sum();
                String gradingLogId = DEV_SEED_PREFIX + kakaoId + "-" + date;

                batch.add(GradingLogMongoEntity.builder()
                        .id(gradingLogId)
                        .userId(kakaoId)
                        .mode(QuizMode.STANDARD)
                        .solvedAt(solvedAt)
                        .totalCount(items.size())
                        .correctCount(correctCount)
                        .results(items)
                        .topicNames(List.of())  // 이름 조회 생략 (성능)
                        .build());

                totalDocs++;
                totalItems += items.size();
            }

            // 배치 플러시
            if (batch.size() >= BATCH_SIZE * quizPerDay || idx == users - 1) {
                if (!batch.isEmpty()) {
                    mongoTemplate.insert(batch, GradingLogMongoEntity.class);
                    progress.advance(idx + 1, totalDocs);
                    log.info("[DevGradingSeedController] 진행: idx={}/{}, docs={}", idx + 1, users, totalDocs);
                    batch.clear();
                }
            }
        }

        progress.finish();
        log.info("[DevGradingSeedController] 완료: docs={}, items={}", totalDocs, totalItems);
        return ApiResponse.onSuccess(new SeedGradingLogsResult(totalDocs, totalItems));
    }

    // ─── 아이템 생성 ─────────────────────────────────────────────────────────

    private List<GradingItem> buildItems(List<QuizAnswerKey> pool, long kakaoId,
                                         int d, int quizPerDay, Random rng) {
        List<GradingItem> items = new ArrayList<>(quizPerDay);
        for (int i = 0; i < quizPerDay; i++) {
            int idx = (int) Math.abs((kakaoId * quizPerDay + (long) d * quizPerDay + i) % pool.size());
            QuizAnswerKey key = pool.get(idx);
            boolean isCorrect = rng.nextInt(3) != 0; // ~67% 정답률
            int score = isCorrect ? (10 + rng.nextInt(6)) : 0;
            items.add(GradingItem.builder()
                    .quizId(key.id())
                    .quizType(key.type())
                    .quizContent("[DEV-SEED]")
                    .correctAnswer(key.correctAnswer())
                    .score(score)
                    .isCorrect(isCorrect)
                    .userAnswer(isCorrect ? key.correctAnswer() : "wrong")
                    .feedback(isCorrect ? "정답입니다." : "오답입니다.")
                    .missingKeywords(List.of())
                    .improvedAnswer(null)
                    .build());
        }
        return items;
    }

    // ─── 답안키 조회 ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<QuizAnswerKey> fetchAnswerKeys() {
        try {
            AnswerKeysApiResponse resp = quizClient.get()
                    .uri(QUIZ_ANSWER_KEY_PATH)
                    .retrieve()
                    .body(AnswerKeysApiResponse.class);
            return (resp != null && resp.data() != null) ? resp.data() : List.of();
        } catch (Exception e) {
            log.warn("[DevGradingSeedController] quiz 답안키 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── 세그먼트 레이아웃 헬퍼 ─────────────────────────────────────────────

    /**
     * 세그먼트 레이아웃 계산.
     *
     * <p>토큰 풀 영역(0..tokenPoolSize-1)은 [poolPower | poolRegular | poolCasual] 순 배치.
     * 배경 영역은 [bgPower | bgRegular | bgCasual] 순 배치.
     * 각 블록 내에서 segIdx=0이 signupWindowDays(최고령), 마지막이 0일(신규).
     */
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

        /** idx에 해당하는 세그먼트의 dpw 반환. */
        int dpwOf(int idx, int powerDpw, int regularDpw, int casualDpw) {
            return switch (segmentOf(idx)) {
                case POWER   -> powerDpw;
                case REGULAR -> regularDpw;
                case CASUAL  -> casualDpw;
            };
        }

        /**
         * idx에 해당하는 계정나이(일) 계산.
         * 세그먼트 블록 내 첫 번째(segIdx=0) → signupWindowDays, 마지막 → 0일.
         */
        int accountAgeDays(int idx, int signupWindowDays) {
            int[] segInfo = segmentIdxAndSize(idx); // [segIdx, segSize]
            int segIdx  = segInfo[0];
            int segSize = segInfo[1];
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

        /** [segIdx within block, block size] */
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

    // ─── 결과 / 내부 레코드 ───────────────────────────────────────────────────

    public  record SeedGradingLogsResult(int docsCreated, int itemsCreated) {}
    private record QuizAnswerKey(Long id, QuizType type, QuizLevel level,
                                 List<Long> topicIds, String correctAnswer) {}
    private record AnswerKeysApiResponse(boolean isSuccess, List<QuizAnswerKey> data) {}
}
