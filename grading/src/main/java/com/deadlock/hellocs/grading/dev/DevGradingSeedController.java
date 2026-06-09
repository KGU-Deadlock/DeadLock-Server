package com.deadlock.hellocs.grading.dev;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.grading.adapter.out.event.GradingCompletedEvent;
import com.deadlock.hellocs.grading.adapter.out.persistence.GradingLogRepository;
import com.deadlock.hellocs.grading.application.port.out.CommandGradingEventOutputPort;
import com.deadlock.hellocs.grading.application.port.out.CommandGradingLogOutputPort;
import com.deadlock.hellocs.grading.application.port.out.QueryTopicOutputPort;
import com.deadlock.hellocs.grading.domain.GradingItem;
import com.deadlock.hellocs.grading.domain.GradingLog;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import com.deadlock.hellocs.quiz.contract.QuizMode;
import com.deadlock.hellocs.quiz.contract.QuizType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;

/**
 * dev 서비스가 호출하는 채점 기록 시딩 엔드포인트. dev 프로파일에서만 활성화된다.
 * <p>
 * grading_logs(MongoDB)에 DEV-SEED 퀴즈를 참조하는 채점 기록을 저장하고,
 * ranking·streak가 소비할 {@code grading.completed} 이벤트를 함께 발행한다.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/v1/internal/dev")
public class DevGradingSeedController {

    private static final String DEV_SEED_ID_PREFIX = "dev-seed-";
    private static final String QUIZ_ANSWER_KEY_PATH = "/v1/internal/dev/quiz-bank/answer-keys";

    private final GradingLogRepository gradingLogRepository;
    private final CommandGradingLogOutputPort commandGradingLogPort;
    private final CommandGradingEventOutputPort commandGradingEventPort;
    private final QueryTopicOutputPort queryTopicPort;
    private final RestClient quizClient;

    public DevGradingSeedController(
            GradingLogRepository gradingLogRepository,
            CommandGradingLogOutputPort commandGradingLogPort,
            CommandGradingEventOutputPort commandGradingEventPort,
            QueryTopicOutputPort queryTopicPort,
            @Value("${service.quiz.url}") String quizServiceUrl) {
        this.gradingLogRepository = gradingLogRepository;
        this.commandGradingLogPort = commandGradingLogPort;
        this.commandGradingEventPort = commandGradingEventPort;
        this.queryTopicPort = queryTopicPort;
        this.quizClient = RestClient.create(quizServiceUrl);
    }

    /**
     * 채점 기록 시딩.
     *
     * @param count     생성할 유저 수 (kakaoId 1001~1000+count 범위, DevUserSeedController와 동일)
     * @param days      시딩할 일수
     * @param fromIndex 증분 시딩 시작 인덱스 (0이면 전체 재시딩, 이미 시딩된 유저 건너뜀)
     */
    @PostMapping("/grading-logs")
    public ApiResponse<SeedGradingLogsResult> seedGradingLogs(
            @RequestParam(name = "count", defaultValue = "10") int count,
            @RequestParam(name = "days", defaultValue = "14") int days,
            @RequestParam(name = "fromIndex", defaultValue = "0") int fromIndex) {

        // 1. quiz-service에서 DEV-SEED 답안키 조회
        List<QuizAnswerKey> answerKeys = fetchAnswerKeys();
        if (answerKeys.isEmpty()) {
            log.warn("[DevGradingSeedController] DEV-SEED 퀴즈 없음. quiz-bank 먼저 시딩하세요.");
            return ApiResponse.onSuccess(new SeedGradingLogsResult(0, 0));
        }

        LocalDate today = LocalDate.now();
        Random random = new Random(42L);
        int logsCreated = 0;
        int eventsPublished = 0;

        int startIdx = Math.max(0, Math.min(fromIndex, count));
        for (int idx = startIdx; idx < count; idx++) {
            long kakaoId = 1000L + idx + 1; // 1001 ~ 1000+count

            for (int d = days - 1; d >= 0; d--) {
                if ((d + idx) % 5 == 0) continue;

                LocalDate date = today.minusDays(d);
                String idPrefix = DEV_SEED_ID_PREFIX + kakaoId + "-" + date + "-";

                // 멱등성: 이미 해당 (유저, 날짜) 로그가 존재하면 skip
                if (gradingLogRepository.existsByIdStartingWith(idPrefix)) {
                    continue;
                }

                LocalDateTime solvedAt = date.atTime(10 + random.nextInt(10), random.nextInt(60));
                int sessionSize = 3 + random.nextInt(3); // 3~5문제

                // 해당 날짜의 토픽 결정 (deterministic)
                List<Long> allTopicIds = answerKeys.stream()
                        .flatMap(k -> k.topicIds().stream())
                        .distinct()
                        .toList();
                Long sessionTopicId = allTopicIds.isEmpty()
                        ? null
                        : allTopicIds.get((idx + d) % allTopicIds.size());
                List<Long> sessionTopicIds = sessionTopicId != null ? List.of(sessionTopicId) : List.of();

                // 해당 토픽의 퀴즈 중 sessionSize개 선택
                List<QuizAnswerKey> pool = answerKeys.stream()
                        .filter(k -> sessionTopicId == null || k.topicIds().contains(sessionTopicId))
                        .toList();
                if (pool.isEmpty()) pool = answerKeys; // fallback
                List<GradingItem> items = buildGradingItems(pool, sessionSize, idx, d, random);

                // topicNames 해석
                List<String> topicNames = sessionTopicIds.isEmpty()
                        ? List.of()
                        : queryTopicPort.getTopicNames(sessionTopicIds);

                int correctCount = (int) items.stream().filter(GradingItem::isCorrect).count();
                int totalScore = items.stream().mapToInt(GradingItem::score).sum();

                String gradingLogId = idPrefix + UUID.randomUUID().toString().substring(0, 8);
                GradingLog gradingLog = GradingLog.builder()
                        .id(gradingLogId)
                        .userId(kakaoId)
                        .mode(QuizMode.STANDARD)
                        .solvedAt(solvedAt)
                        .totalCount(items.size())
                        .correctCount(correctCount)
                        .results(items)
                        .topicNames(topicNames)
                        .build();

                commandGradingLogPort.save(gradingLog);
                logsCreated++;

                commandGradingEventPort.publish(new GradingCompletedEvent(
                        gradingLogId,
                        kakaoId,
                        solvedAt,
                        items.size(),
                        totalScore,
                        sessionTopicIds
                ));
                eventsPublished++;
            }
        }

        log.info("[DevGradingSeedController] grading_logs 생성={}, 이벤트 발행={}", logsCreated, eventsPublished);
        return ApiResponse.onSuccess(new SeedGradingLogsResult(logsCreated, eventsPublished));
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private List<GradingItem> buildGradingItems(List<QuizAnswerKey> pool, int size,
                                                 int idx, int d, Random random) {
        List<GradingItem> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            QuizAnswerKey key = pool.get((idx * 100 + d * 10 + i) % pool.size());
            // 3문제 중 약 2/3 정답 분포
            boolean isCorrect = random.nextInt(3) != 0;
            int score = isCorrect ? (10 + random.nextInt(6)) : 0; // 10~15점 or 0점
            String userAnswer = isCorrect ? key.correctAnswer() : "wrong-answer-" + i;

            items.add(GradingItem.builder()
                    .quizId(key.id())
                    .quizType(key.type())
                    .quizContent("[DEV-SEED] quiz content " + key.id())
                    .correctAnswer(key.correctAnswer())
                    .score(score)
                    .isCorrect(isCorrect)
                    .userAnswer(userAnswer)
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

    // ─── 내부 레코드 ──────────────────────────────────────────────────────────

    public record SeedGradingLogsResult(int logsCreated, int eventsPublished) {}

    private record QuizAnswerKey(Long id, QuizType type, QuizLevel level, List<Long> topicIds, String correctAnswer) {}

    private record AnswerKeysApiResponse(boolean isSuccess, List<QuizAnswerKey> data) {}
}
