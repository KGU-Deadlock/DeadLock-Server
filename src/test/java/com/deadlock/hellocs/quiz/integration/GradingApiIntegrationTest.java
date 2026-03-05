package com.deadlock.hellocs.quiz.integration;

import com.deadlock.hellocs.quiz.grading.adapter.out.persistence.GradingLogRepository;
import com.deadlock.hellocs.quiz.grading.adapter.out.persistence.entity.GradingLogMongoEntity;
import com.deadlock.hellocs.quiz.grading.application.port.out.CommandAiGradingOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.QuizRepository;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizMultipleChoiceJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizOxJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizShortAnswerJpaEntity;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.ranking.adapter.in.event.RankingGradingCompletedEventListener;
import com.deadlock.hellocs.streak.adapter.in.event.StreakGradingCompletedEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GradingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizRepository quizRepository;

    @MockitoBean
    private GradingLogRepository gradingLogRepository;

    @MockitoBean
    private RankingGradingCompletedEventListener rankingGradingCompletedEventListener;

    @MockitoBean
    private StreakGradingCompletedEventListener streakGradingCompletedEventListener;

    @MockitoBean
    private CommandAiGradingOutputPort commandAiGradingOutputPort;

    private final Map<String, GradingLogMongoEntity> storedLogs = new ConcurrentHashMap<>();

    private Long oxQuizId;
    private Long multipleChoiceQuizId;
    private Long shortAnswerQuizId;

    @BeforeEach
    void setUp() {
        storedLogs.clear();
        quizRepository.deleteAll();
        seedQuizzes();
        configureGradingLogRepositoryDouble();
        configureAiGradingDouble();
    }

    @Test
    void submitAndQueryGradingLogRunsEndToEnd() throws Exception {
        String requestBody = objectMapper.writeValueAsString(List.of(
                Map.of("quizId", oxQuizId, "answer", "true"),
                Map.of("quizId", multipleChoiceQuizId, "answer", "1"),
                Map.of("quizId", shortAnswerQuizId, "answer", "deadlock")
        ));

        String submitResponseBody = mockMvc.perform(post("/api/v1/quiz/grading")
                        .with(jwt().jwt(jwt -> jwt.subject("777")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode submitResponse = objectMapper.readTree(submitResponseBody);
        String gradingLogId = submitResponse.get("data").get("gradingLogId").asText();
        assertNotNull(gradingLogId);

        String gradingLogResponseBody = mockMvc.perform(get("/api/v1/quiz/grading/{gradingLogId}", gradingLogId)
                        .with(jwt().jwt(jwt -> jwt.subject("777"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode gradingLogResponse = objectMapper.readTree(gradingLogResponseBody);
        JsonNode gradingLogData = gradingLogResponse.get("data");

        assertTrue(gradingLogResponse.get("isSuccess").asBoolean());
        assertEquals(3, gradingLogData.get("quizCount").asInt());
        assertEquals(2, gradingLogData.get("correctCount").asInt());
        assertEquals(3, gradingLogData.get("gradingResults").size());

        String detailResponseBody = mockMvc.perform(
                        get("/api/v1/quiz/grading/{gradingLogId}/{quizId}", gradingLogId, multipleChoiceQuizId)
                                .with(jwt().jwt(jwt -> jwt.subject("777"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode detailResponse = objectMapper.readTree(detailResponseBody);
        JsonNode detailData = detailResponse.get("data");

        assertEquals(0, detailData.get("score").asInt());
        assertEquals(false, detailData.get("isCorrect").asBoolean());
        assertEquals("객관식", detailData.get("quizType").asText());
        assertEquals("2", detailData.get("correctAnswer").asText());
    }

    @Test
    void submitWithDuplicatedQuizIdsReturnsGradingRequestInvalid() throws Exception {
        String requestBody = objectMapper.writeValueAsString(List.of(
                Map.of("quizId", oxQuizId, "answer", "true"),
                Map.of("quizId", oxQuizId, "answer", "false")
        ));

        String responseBody = mockMvc.perform(post("/api/v1/quiz/grading")
                        .with(jwt().jwt(jwt -> jwt.subject("777")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        assertEquals("GRADING4001", response.get("code").asText());
    }

    @Test
    void submitWithBlankAnswerReturnsGradingRequestInvalid() throws Exception {
        String requestBody = objectMapper.writeValueAsString(List.of(
                Map.of("quizId", shortAnswerQuizId, "answer", "   ")
        ));

        String responseBody = mockMvc.perform(post("/api/v1/quiz/grading")
                        .with(jwt().jwt(jwt -> jwt.subject("777")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        assertEquals("GRADING4001", response.get("code").asText());
    }

    private void configureGradingLogRepositoryDouble() {
        when(gradingLogRepository.save(any(GradingLogMongoEntity.class))).thenAnswer(invocation -> {
            GradingLogMongoEntity source = invocation.getArgument(0);
            String id = source.getId();
            if (id == null || id.isBlank()) {
                id = "log-" + UUID.randomUUID();
            }

            GradingLogMongoEntity saved = GradingLogMongoEntity.builder()
                    .id(id)
                    .userId(source.getUserId())
                    .solvedAt(source.getSolvedAt())
                    .totalCount(source.getTotalCount())
                    .correctCount(source.getCorrectCount())
                    .results(source.getResults())
                    .build();
            storedLogs.put(id, saved);
            return saved;
        });

        when(gradingLogRepository.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(storedLogs.get(invocation.getArgument(0)))
        );
    }

    private void configureAiGradingDouble() {
        when(commandAiGradingOutputPort.gradeWithAi(any(Quiz.class), anyString())).thenAnswer(invocation -> {
            Quiz quiz = invocation.getArgument(0);
            String userAnswer = invocation.getArgument(1);
            boolean isCorrect = quiz.getAnswer().asString().equalsIgnoreCase(userAnswer);

            return GradingItem.builder()
                    .quizId(quiz.getId())
                    .score(isCorrect ? 100 : 0)
                    .isCorrect(isCorrect)
                    .userAnswer(userAnswer)
                    .feedback("ai-feedback")
                    .missingKeywords(List.of())
                    .improvedAnswer(null)
                    .build();
        });
    }

    private void seedQuizzes() {
        QuizOxJpaEntity oxQuiz = quizRepository.save(
                QuizOxJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(201L))
                        .content("ox-question")
                        .answer(true)
                        .explain("ox-explain")
                        .build()
        );
        oxQuizId = oxQuiz.getId();

        QuizMultipleChoiceJpaEntity multipleChoiceQuiz = quizRepository.save(
                QuizMultipleChoiceJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(201L))
                        .content("multiple-choice-question")
                        .answer(2)
                        .choice("multiple-choice-1||multiple-choice-2||multiple-choice-3||multiple-choice-4")
                        .explain("multiple-choice-explain")
                        .build()
        );
        multipleChoiceQuizId = multipleChoiceQuiz.getId();

        QuizShortAnswerJpaEntity shortAnswerQuiz = quizRepository.save(
                QuizShortAnswerJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(201L))
                        .content("short-answer-question")
                        .answer("deadlock")
                        .explain("short-answer-explain")
                        .build()
        );
        shortAnswerQuizId = shortAnswerQuiz.getId();
    }
}
