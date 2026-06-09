package com.deadlock.hellocs.quiz.dev;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.adapter.out.persistence.QuizRepository;
import com.deadlock.hellocs.quiz.adapter.out.persistence.entity.QuizJpaEntity;
import com.deadlock.hellocs.quiz.adapter.out.persistence.entity.QuizMultipleChoiceJpaEntity;
import com.deadlock.hellocs.quiz.adapter.out.persistence.entity.QuizOxJpaEntity;
import com.deadlock.hellocs.quiz.adapter.out.persistence.entity.QuizShortAnswerJpaEntity;
import com.deadlock.hellocs.quiz.adapter.out.persistence.entity.QuizVoiceJpaEntity;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import com.deadlock.hellocs.quiz.contract.QuizType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * dev 서비스가 호출하는 퀴즈 뱅크 시딩 엔드포인트. dev 프로파일에서만 활성화된다.
 * topic service REST API를 통해 토픽 ID를 조회하므로 :topic 모듈에 컴파일 결합 없음.
 */
@Slf4j
@RestController
@RequestMapping("/v1/internal/dev")
public class DevQuizSeedController {

    private static final String DEV_SEED_PREFIX = "[DEV-SEED]";
    private static final List<String> SEED_TOPIC_NAMES = List.of(
            "Network", "OS", "Database", "Java", "Spring", "Algorithm"
    );

    private final QuizRepository quizRepository;
    private final RestClient topicClient;

    public DevQuizSeedController(
            QuizRepository quizRepository,
            @Value("${service.topic.url}") String topicServiceUrl) {
        this.quizRepository = quizRepository;
        this.topicClient = RestClient.create(topicServiceUrl);
    }

    @PostMapping("/quiz-bank")
    @Transactional
    public ApiResponse<SeedQuizBankResult> seedQuizBank(
            @RequestParam(name = "perCombo", defaultValue = "5") int perCombo) {

        // 토픽 서비스에서 ID 조회 (ids?names=...)
        TopicIdsApiResponse topicIdsResp = topicClient.get()
                .uri(uri -> uri.path("/v1/topics/ids")
                        .queryParam("names", SEED_TOPIC_NAMES.toArray())
                        .build())
                .retrieve()
                .body(TopicIdsApiResponse.class);

        List<Long> topicIds = (topicIdsResp != null && topicIdsResp.data() != null)
                ? topicIdsResp.data() : List.of();

        if (topicIds.isEmpty()) {
            log.warn("[DevQuizSeedController] 토픽 ID 조회 결과 없음. 토픽 먼저 시딩하세요.");
            return ApiResponse.onSuccess(new SeedQuizBankResult(0, 0));
        }

        // topicId와 name을 매핑: 인덱스 순서대로 SEED_TOPIC_NAMES와 대응
        // topic service의 /ids?names= 엔드포인트는 names 순서대로 ID 반환
        int topicCount = Math.min(SEED_TOPIC_NAMES.size(), topicIds.size());
        List<QuizJpaEntity> toCreate = new ArrayList<>();

        for (int i = 0; i < topicCount; i++) {
            String topicName = SEED_TOPIC_NAMES.get(i);
            Long topicId = topicIds.get(i);

            for (QuizLevel level : QuizLevel.values()) {
                for (QuizType type : QuizType.values()) {
                    long existing = quizRepository.countDevSeedByLevelAndTypeAndTopicId(
                            level, type, topicId, DEV_SEED_PREFIX);
                    int missing = Math.max(0, perCombo - (int) existing);
                    for (int seq = (int) existing + 1; seq <= (int) existing + missing; seq++) {
                        toCreate.add(buildQuiz(topicName, topicId, level, type, seq));
                    }
                }
            }
        }

        if (!toCreate.isEmpty()) {
            quizRepository.saveAll(toCreate);
        }

        return ApiResponse.onSuccess(new SeedQuizBankResult(topicCount, toCreate.size()));
    }

    private QuizJpaEntity buildQuiz(String topicName, Long topicId,
                                    QuizLevel level, QuizType type, int sequence) {
        return switch (type) {
            case OX -> buildOxQuiz(topicName, topicId, level, sequence);
            case MULTIPLE_CHOICE -> buildMultipleChoiceQuiz(topicName, topicId, level, sequence);
            case SHORT_ANSWER -> buildShortAnswerQuiz(topicName, topicId, level, sequence);
            case VOICE -> buildVoiceQuiz(topicName, topicId, level, sequence);
        };
    }

    private QuizOxJpaEntity buildOxQuiz(String topicName, Long topicId,
                                         QuizLevel level, int sequence) {
        boolean answer = sequence % 2 == 1;
        return QuizOxJpaEntity.builder()
                .level(level)
                .topicIds(List.of(topicId))
                .content(DEV_SEED_PREFIX + "[" + topicName + "][" + level.name() + "][OX][" + sequence + "] "
                        + topicName + " statement " + sequence + " is " + (answer ? "correct" : "incorrect") + ".")
                .answer(answer)
                .explain(topicName + " " + level.name() + " OX explanation " + sequence + ".")
                .build();
    }

    private QuizMultipleChoiceJpaEntity buildMultipleChoiceQuiz(String topicName, Long topicId,
                                                                  QuizLevel level, int sequence) {
        int answer = (sequence % 4) + 1;
        return QuizMultipleChoiceJpaEntity.builder()
                .level(level)
                .topicIds(List.of(topicId))
                .content(DEV_SEED_PREFIX + "[" + topicName + "][" + level.name()
                        + "][MULTIPLE_CHOICE][" + sequence + "] "
                        + topicName + " multiple choice question " + sequence + ".")
                .answer(answer)
                .choice(topicName + " " + level.name() + " choice " + sequence + "-1"
                        + "||" + topicName + " " + level.name() + " choice " + sequence + "-2"
                        + "||" + topicName + " " + level.name() + " choice " + sequence + "-3"
                        + "||" + topicName + " " + level.name() + " choice " + sequence + "-4")
                .explain(topicName + " " + level.name() + " multiple choice explanation " + sequence + ".")
                .build();
    }

    private QuizShortAnswerJpaEntity buildShortAnswerQuiz(String topicName, Long topicId,
                                                           QuizLevel level, int sequence) {
        return QuizShortAnswerJpaEntity.builder()
                .level(level)
                .topicIds(List.of(topicId))
                .content(DEV_SEED_PREFIX + "[" + topicName + "][" + level.name()
                        + "][SHORT_ANSWER][" + sequence + "] "
                        + topicName + " short answer question " + sequence + ".")
                .answer(topicName + " " + level.name() + " keyword " + sequence)
                .explain(topicName + " " + level.name() + " short answer explanation " + sequence + ".")
                .build();
    }

    private QuizVoiceJpaEntity buildVoiceQuiz(String topicName, Long topicId,
                                               QuizLevel level, int sequence) {
        String voiceKey = topicName.toLowerCase(Locale.ROOT).replace(" ", "-");
        String levelLabel = level.name().toLowerCase(Locale.ROOT);
        return QuizVoiceJpaEntity.builder()
                .level(level)
                .topicIds(List.of(topicId))
                .content("voice://dev-seed/" + voiceKey + "/" + levelLabel + "/question-" + sequence)
                .contentText(DEV_SEED_PREFIX + "[" + topicName + "][" + level.name()
                        + "][VOICE][" + sequence + "] "
                        + topicName + " voice question " + sequence + ".")
                .answer(topicName + " " + level.name() + " voice answer " + sequence)
                .explain(topicName + " " + level.name() + " voice explanation " + sequence + ".")
                .build();
    }

    /**
     * DEV-SEED 퀴즈의 답안키 목록 반환.
     * grading-service의 DevGradingSeedController가 채점 기록 시딩에 사용한다.
     */
    @GetMapping("/quiz-bank/answer-keys")
    @Transactional(readOnly = true)
    public ApiResponse<List<QuizAnswerKey>> getDevSeedAnswerKeys() {
        List<QuizAnswerKey> keys = quizRepository.findAllDevSeed(DEV_SEED_PREFIX).stream()
                .map(q -> new QuizAnswerKey(
                        q.getId(),
                        q.getType(),
                        q.getLevel(),
                        q.getTopicIds(),
                        q.correctAnswerAsString()
                ))
                .toList();
        return ApiResponse.onSuccess(keys);
    }

    // ─── 응답 역직렬화용 레코드 ─────────────────────────────────────────────────

    private record TopicIdsApiResponse(boolean isSuccess, List<Long> data) {}

    public record SeedQuizBankResult(int topicCount, int quizzesCreated) {}

    public record QuizAnswerKey(
            Long id,
            QuizType type,
            QuizLevel level,
            List<Long> topicIds,
            String correctAnswer
    ) {}
}
