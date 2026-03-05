package com.deadlock.hellocs.quiz.integration;

import com.deadlock.hellocs.quiz.grading.adapter.out.persistence.GradingLogRepository;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.QuizRepository;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizMultipleChoiceJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizOxJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizShortAnswerJpaEntity;
import com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity.QuizVoiceJpaEntity;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import com.deadlock.hellocs.user.adapter.out.persistence.UserRepository;
import com.deadlock.hellocs.user.adapter.out.persistence.entity.UserJpaJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuizApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GradingLogRepository gradingLogRepository;

    @BeforeEach
    void setUp() {
        quizRepository.deleteAll();
        userRepository.deleteAll();
        seedUsers();
        seedQuizzes();
    }

    @Test
    void getQuizStandardModeReturnsComposedQuizzes() throws Exception {
        String responseBody = mockMvc.perform(get("/api/v1/quiz")
                        .param("topicIds", "101", "102")
                        .param("mode", "STANDARD")
                        .with(jwt().jwt(jwt -> jwt.subject("100"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        JsonNode data = response.get("data");

        assertTrue(response.get("isSuccess").asBoolean());
        assertEquals("COMMON2000", response.get("code").asText());
        assertNotNull(data);
        assertEquals(5, data.size());

        Map<String, Integer> typeCount = countQuizTypes(data);
        assertEquals(2, typeCount.getOrDefault(QuizType.OX.name(), 0));
        assertEquals(2, typeCount.getOrDefault(QuizType.MULTIPLE_CHOICE.name(), 0));
        assertEquals(1, typeCount.getOrDefault(QuizType.SHORT_ANSWER.name(), 0));

        for (JsonNode quiz : data) {
            JsonNode choices = quiz.get("choices");
            assertNotNull(choices);
            if (QuizType.MULTIPLE_CHOICE.name().equals(quiz.get("type").asText())) {
                assertTrue(choices.isArray());
                assertEquals(4, choices.size());
            } else {
                assertTrue(choices.isEmpty());
            }
        }
    }

    @Test
    void getQuizVoiceModeReturnsOnlyVoiceQuizzes() throws Exception {
        String responseBody = mockMvc.perform(get("/api/v1/quiz")
                        .param("topicIds", "101")
                        .param("mode", "VOICE")
                        .with(jwt().jwt(jwt -> jwt.subject("100"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        JsonNode data = response.get("data");

        assertTrue(response.get("isSuccess").asBoolean());
        assertNotNull(data);
        assertEquals(3, data.size());

        for (JsonNode quiz : data) {
            assertEquals(QuizType.VOICE.name(), quiz.get("type").asText());
        }
    }

    @Test
    void getQuizWithoutTopicIdsReturnsQuizRequestInvalid() throws Exception {
        String responseBody = mockMvc.perform(get("/api/v1/quiz")
                        .param("mode", "STANDARD")
                        .with(jwt().jwt(jwt -> jwt.subject("100"))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        assertEquals("QUIZ4001", response.get("code").asText());
    }

    private Map<String, Integer> countQuizTypes(JsonNode quizzes) {
        Map<String, Integer> count = new HashMap<>();
        for (JsonNode quiz : quizzes) {
            String type = quiz.get("type").asText();
            count.put(type, count.getOrDefault(type, 0) + 1);
        }
        return count;
    }

    private void seedQuizzes() {
        quizRepository.saveAll(List.of(
                QuizOxJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(101L))
                        .content("ox-1")
                        .answer(true)
                        .explain("ox-1 explain")
                        .build(),
                QuizOxJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(102L))
                        .content("ox-2")
                        .answer(false)
                        .explain("ox-2 explain")
                        .build(),
                QuizMultipleChoiceJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(101L))
                        .content("mc-1")
                        .answer(2)
                        .choice("mc-1-a||mc-1-b||mc-1-c||mc-1-d")
                        .explain("mc-1 explain")
                        .build(),
                QuizMultipleChoiceJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(102L))
                        .content("mc-2")
                        .answer(3)
                        .choice("mc-2-a||mc-2-b||mc-2-c||mc-2-d")
                        .explain("mc-2 explain")
                        .build(),
                QuizShortAnswerJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(101L, 102L))
                        .content("sa-1")
                        .answer("deadlock")
                        .explain("sa-1 explain")
                        .build(),
                QuizVoiceJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(101L))
                        .content("voice-1")
                        .answer("voice-a1")
                        .explain("voice-1 explain")
                        .contentText("voice-text-1")
                        .build(),
                QuizVoiceJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(101L))
                        .content("voice-2")
                        .answer("voice-a2")
                        .explain("voice-2 explain")
                        .contentText("voice-text-2")
                        .build(),
                QuizVoiceJpaEntity.builder()
                        .level(QuizLevel.PRO)
                        .topicIds(List.of(101L))
                        .content("voice-3")
                        .answer("voice-a3")
                        .explain("voice-3 explain")
                        .contentText("voice-text-3")
                        .build()
        ));
    }

    private void seedUsers() {
        String nickname = "u" + UUID.randomUUID().toString().substring(0, 8);
        userRepository.save(
                UserJpaJpaEntity.builder()
                        .nickname(nickname)
                        .kakaoEmail("quiz-test-user@example.com")
                        .kakaoId(100L)
                        .quizLevel(QuizLevel.PRO)
                        .build()
        );
    }
}
