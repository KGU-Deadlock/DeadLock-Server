package com.deadlock.hellocs.quiz.grading.adapter.in.web;

import com.deadlock.hellocs.global.config.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.quiz.exception.QuizExceptionHandler;
import com.deadlock.hellocs.quiz.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.QueryGradingLogInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingItemResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.SubmitAnswersCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizGradingController.class)
@Import({SecurityConfig.class, QuizExceptionHandler.class})
@ActiveProfiles("test")
class QuizGradingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommandAnswerInputPort commandAnswerInputPort;

    @MockitoBean
    private QueryGradingLogInputPort queryGradingLogInputPort;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("POST /v1/quiz/grading - 인증된 사용자는 답안을 제출할 수 있다")
    void submitAnswers_success() throws Exception {
        List<UserGradingCommand> answers = List.of(
                new UserGradingCommand(101L, "true"),
                new UserGradingCommand(102L, "객체지향")
        );

        given(commandAnswerInputPort.submit(eq(new SubmitAnswersCommand(12345L, answers))))
                .willReturn("log-12345");

        mockMvc.perform(post("/v1/quiz/grading")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "quizId": 101, "answer": "true" },
                                  { "quizId": 102, "answer": "객체지향" }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.gradingLogId").value("log-12345"));

        then(commandAnswerInputPort).should()
                .submit(new SubmitAnswersCommand(12345L, answers));
    }

    @Test
    @DisplayName("POST /v1/quiz/grading - 잘못된 답안 요청이면 400을 반환한다")
    void submitAnswers_invalidRequest() throws Exception {
        mockMvc.perform(post("/v1/quiz/grading")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "quizId": 101, "answer": "" }
                                ]
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("GRADING4001"))
                .andExpect(jsonPath("$.data").doesNotExist());

        then(commandAnswerInputPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /v1/quiz/grading - 인증되지 않은 요청이면 401을 반환한다")
    void submitAnswers_unauthorized() throws Exception {
        mockMvc.perform(post("/v1/quiz/grading")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "quizId": 101, "answer": "true" }
                                ]
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON401"))
                .andExpect(jsonPath("$.data").value((Object) null));

        then(commandAnswerInputPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /v1/quiz/grading/{gradingLogId} - 채점 로그를 조회할 수 있다")
    void getGradingLog_success() throws Exception {
        GradingLogResult result = GradingLogResult.builder()
                .correctCount(1)
                .quizCount(2)
                .gradingResults(List.of(
                        GradingItemResult.builder()
                                .quizId(101L)
                                .content("자바는 클래스 기반 언어인가?")
                                .quizType("OX")
                                .isCorrect(true)
                                .build()
                ))
                .build();

        given(queryGradingLogInputPort.getGradingLog(new GetGradingLogCommand("log-12345")))
                .willReturn(result);

        mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}", "log-12345")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.correctCount").value(1))
                .andExpect(jsonPath("$.data.quizCount").value(2))
                .andExpect(jsonPath("$.data.gradingResults[0].quizId").value(101))
                .andExpect(jsonPath("$.data.gradingResults[0].isCorrect").value(true));

        then(queryGradingLogInputPort).should()
                .getGradingLog(new GetGradingLogCommand("log-12345"));
    }

    @Test
    @DisplayName("GET /v1/quiz/grading/{gradingLogId}/{quizId} - 채점 상세를 조회할 수 있다")
    void getGradingDetailLog_success() throws Exception {
        GradingDetailLogResult result = GradingDetailLogResult.builder()
                .quizId(101L)
                .score(100)
                .isCorrect(true)
                .content("자바는 클래스 기반 언어인가?")
                .quizType("OX")
                .userAnswer("true")
                .correctAnswer("true")
                .feedback("정답입니다.")
                .missingKeywords(List.of())
                .improvedAnswer("true")
                .build();

        given(queryGradingLogInputPort.getGradingDetailLog(new GetGradingDetailLogCommand("log-12345", 101L)))
                .willReturn(result);

        mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}/{quizId}", "log-12345", 101L)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.quizId").value(101))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.isCorrect").value(true))
                .andExpect(jsonPath("$.data.correctAnswer").value("true"));

        then(queryGradingLogInputPort).should()
                .getGradingDetailLog(new GetGradingDetailLogCommand("log-12345", 101L));
    }
}
