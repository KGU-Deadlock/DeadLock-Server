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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/v1/internal/dev")
public class DevGradingSeedController {

    private static final String DEV_SEED_PREFIX      = "dev-seed-";
    private static final String QUIZ_ANSWER_KEY_PATH = "/v1/internal/dev/quiz-bank/answer-keys";
    private static final int    BATCH_SIZE           = 500;

    private final MongoTemplate   mongoTemplate;
    private final RestClient      quizClient;
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

    @PostMapping("/grading-logs")
    public ApiResponse<SeedGradingLogsResult> seedGradingLogs(@RequestBody SeedRequest request) {

        List<QuizAnswerKey> answerKeys = fetchAnswerKeys();
        if (answerKeys.isEmpty()) {
            log.warn("[DevGradingSeedController] DEV-SEED 퀴즈 없음. quiz-bank 먼저 시딩하세요.");
            return ApiResponse.onSuccess(new SeedGradingLogsResult(0, 0));
        }

        List<UserSpec> specs = request.users();
        progress.start(specs.size());

        LocalDate today    = LocalDate.now();
        int totalDocs      = 0;
        int totalItems     = 0;
        List<GradingLogMongoEntity> batch = new ArrayList<>(BATCH_SIZE * 200);

        for (int idx = 0; idx < specs.size(); idx++) {
            UserSpec spec    = specs.get(idx);
            long  kakaoId    = spec.kakaoId();
            int   accountAge = spec.accountAgeDays();
            Random rng       = new Random(request.seed() + kakaoId * 10_000L);

            for (int d = accountAge - 1; d >= 0; d--) {
                if ((d % 7) >= spec.dpw()) continue;

                LocalDate date         = today.minusDays(d);
                LocalDateTime solvedAt = date.atTime(8 + rng.nextInt(14), rng.nextInt(60));
                List<GradingItem> items = buildItems(answerKeys, kakaoId, d, request.quizPerDay(), rng);

                int correctCount = (int) items.stream().filter(GradingItem::isCorrect).count();
                String gradingLogId = DEV_SEED_PREFIX + kakaoId + "-" + date;

                batch.add(GradingLogMongoEntity.builder()
                        .id(gradingLogId)
                        .userId(kakaoId)
                        .mode(QuizMode.STANDARD)
                        .solvedAt(solvedAt)
                        .totalCount(items.size())
                        .correctCount(correctCount)
                        .results(items)
                        .topicNames(List.of())
                        .build());

                totalDocs++;
                totalItems += items.size();
            }

            if (batch.size() >= BATCH_SIZE * request.quizPerDay() || idx == specs.size() - 1) {
                if (!batch.isEmpty()) {
                    mongoTemplate.insert(batch, GradingLogMongoEntity.class);
                    progress.advance(idx + 1, totalDocs);
                    log.info("[DevGradingSeedController] 진행: idx={}/{}, docs={}", idx + 1, specs.size(), totalDocs);
                    batch.clear();
                }
            }
        }

        progress.finish();
        log.info("[DevGradingSeedController] 완료: docs={}, items={}", totalDocs, totalItems);
        return ApiResponse.onSuccess(new SeedGradingLogsResult(totalDocs, totalItems));
    }

    private List<GradingItem> buildItems(List<QuizAnswerKey> pool, long kakaoId,
                                         int d, int quizPerDay, Random rng) {
        List<GradingItem> items = new ArrayList<>(quizPerDay);
        for (int i = 0; i < quizPerDay; i++) {
            int idx = rng.nextInt(pool.size());
            QuizAnswerKey key = pool.get(idx);
            boolean isCorrect = rng.nextInt(3) != 0;
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

    // ─── 요청/결과/내부 레코드 ───────────────────────────────────────────────

    record UserSpec(long kakaoId, int dpw, int accountAgeDays) {}
    record SeedRequest(List<UserSpec> users, int quizPerDay, int numTopics, long seed) {}

    public  record SeedGradingLogsResult(int docsCreated, int itemsCreated) {}
    private record QuizAnswerKey(Long id, QuizType type, QuizLevel level,
                                 List<Long> topicIds, String correctAnswer) {}
    private record AnswerKeysApiResponse(boolean isSuccess, List<QuizAnswerKey> data) {}
}
