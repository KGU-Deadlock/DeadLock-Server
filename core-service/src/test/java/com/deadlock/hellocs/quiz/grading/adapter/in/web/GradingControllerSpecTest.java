package com.deadlock.hellocs.quiz.grading.adapter.in.web;

import com.deadlock.hellocs.global.auth.handler.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.exception.QuizExceptionHandler;
import com.deadlock.hellocs.quiz.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.QueryGradingLogInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingItemResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogListResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.SubmitAnswersCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizGradingController.class)
@Import({SecurityConfig.class, QuizExceptionHandler.class})
@ActiveProfiles("test")
class GradingControllerSpecTest {

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

    // =========================================================================
    // API 1: POST /v1/quiz/grading — 채점 제출
    // =========================================================================

    @Nested
    @DisplayName("API 1: POST /v1/quiz/grading — 채점 제출")
    class SubmitAnswers {

        @Test
        @DisplayName("GRD-1-01: 유효한 답안 1개 제출 성공")
        void submitSingleAnswer_success() throws Exception {
            List<UserGradingCommand> answers = List.of(new UserGradingCommand(1L, "정답"));

            given(commandAnswerInputPort.submit(eq(new SubmitAnswersCommand(12345L, answers))))
                    .willReturn("log-12345");

            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":"정답"}]
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.gradingLogId").value("log-12345"));

            then(commandAnswerInputPort).should()
                    .submit(new SubmitAnswersCommand(12345L, answers));
        }

        @Test
        @DisplayName("GRD-1-02: 여러 문제 답안 동시 제출 성공")
        void submitMultipleAnswers_success() throws Exception {
            List<UserGradingCommand> answers = List.of(
                    new UserGradingCommand(1L, "O"),
                    new UserGradingCommand(2L, "네트워크"),
                    new UserGradingCommand(3L, "TCP")
            );

            given(commandAnswerInputPort.submit(eq(new SubmitAnswersCommand(12345L, answers))))
                    .willReturn("log-67890");

            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":"O"},{"quizId":2,"answer":"네트워크"},{"quizId":3,"answer":"TCP"}]
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.gradingLogId").value("log-67890"));

            then(commandAnswerInputPort).should()
                    .submit(new SubmitAnswersCommand(12345L, answers));
        }

        @Test
        @DisplayName("GRD-1-03: quizId가 null인 경우 검증 실패")
        void submitWithNullQuizId_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":null,"answer":"정답"}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-04: quizId가 0인 경우 검증 실패")
        void submitWithZeroQuizId_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":0,"answer":"정답"}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-05: quizId가 음수인 경우 검증 실패")
        void submitWithNegativeQuizId_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":-1,"answer":"정답"}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-06: answer가 null인 경우 검증 실패")
        void submitWithNullAnswer_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":null}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-07: answer가 빈 문자열인 경우 검증 실패")
        void submitWithEmptyAnswer_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":""}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-08: answer가 공백 문자열인 경우 검증 실패")
        void submitWithBlankAnswer_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":"   "}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-09: 빈 리스트 제출 시 검증 실패")
        void submitEmptyList_validationFail() throws Exception {
            given(commandAnswerInputPort.submit(eq(new SubmitAnswersCommand(12345L, List.of()))))
                    .willThrow(new CustomException(QuizErrorStatus.GRADING_REQUEST_INVALID));

            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    []
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));
        }

        @Test
        @DisplayName("GRD-1-10: 존재하지 않는 quizId로 제출 시 404")
        void submitWithNonexistentQuizId_notFound() throws Exception {
            List<UserGradingCommand> answers = List.of(new UserGradingCommand(99999L, "정답"));

            given(commandAnswerInputPort.submit(eq(new SubmitAnswersCommand(12345L, answers))))
                    .willThrow(new CustomException(QuizErrorStatus.GRADING_QUIZ_NOT_FOUND));

            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":99999,"answer":"정답"}]
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4043"));

            then(commandAnswerInputPort).should()
                    .submit(new SubmitAnswersCommand(12345L, answers));
        }

        @Test
        @DisplayName("GRD-1-11: JWT 토큰 없이 요청 시 401")
        void submitWithoutJwt_unauthorized() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":"정답"}]
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-12: 리스트 중 일부 quizId=null 시 전체 검증 실패")
        void submitWithPartialNullQuizId_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":"O"},{"quizId":null,"answer":"X"}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-13: 리스트 중 일부 quizId=0 시 전체 검증 실패")
        void submitWithPartialZeroQuizId_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":"O"},{"quizId":0,"answer":"X"}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-14: 리스트 중 일부 answer=null 시 전체 검증 실패")
        void submitWithPartialNullAnswer_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":"O"},{"quizId":2,"answer":null}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-15: 리스트 중 일부 answer=빈문자열 시 전체 검증 실패")
        void submitWithPartialEmptyAnswer_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":"O"},{"quizId":2,"answer":""}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GRD-1-16: 정상/비정상 요소 혼합 시 전체 검증 실패")
        void submitWithMixedValidInvalid_validationFail() throws Exception {
            mockMvc.perform(post("/v1/quiz/grading")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [{"quizId":1,"answer":"O"},{"quizId":0,"answer":"X"},{"quizId":3,"answer":null}]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4001"));

            then(commandAnswerInputPort).shouldHaveNoInteractions();
        }
    }

    // =========================================================================
    // API 2: GET /v1/quiz/grading/{gradingLogId} — 채점 기록 조회
    // =========================================================================

    @Nested
    @DisplayName("API 2: GET /v1/quiz/grading/{gradingLogId} — 채점 기록 조회")
    class GetGradingLog {

        @Test
        @DisplayName("GRD-2-01: 유효한 gradingLogId로 채점 기록 조회 성공")
        void getGradingLog_success() throws Exception {
            GradingLogResult result = GradingLogResult.builder()
                    .correctCount(2)
                    .quizCount(3)
                    .gradingResults(List.of(
                            GradingItemResult.builder()
                                    .quizId(1L)
                                    .content("TCP는 연결 지향 프로토콜인가?")
                                    .quizType("OX")
                                    .isCorrect(true)
                                    .build(),
                            GradingItemResult.builder()
                                    .quizId(2L)
                                    .content("OSI 7계층에서 전송 계층은?")
                                    .quizType("단답형")
                                    .isCorrect(true)
                                    .build()
                    ))
                    .build();

            given(queryGradingLogInputPort.getGradingLog(12345L, new GetGradingLogCommand("valid-log-id")))
                    .willReturn(result);

            mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}", "valid-log-id")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.correctCount").value(2))
                    .andExpect(jsonPath("$.data.quizCount").value(3))
                    .andExpect(jsonPath("$.data.gradingResults").isArray())
                    .andExpect(jsonPath("$.data.gradingResults.length()").value(2));

            then(queryGradingLogInputPort).should()
                    .getGradingLog(12345L, new GetGradingLogCommand("valid-log-id"));
        }

        @Test
        @DisplayName("GRD-2-03: 존재하지 않는 gradingLogId로 조회 시 404")
        void getGradingLog_notFound() throws Exception {
            given(queryGradingLogInputPort.getGradingLog(12345L, new GetGradingLogCommand("nonexistent_id")))
                    .willThrow(new CustomException(QuizErrorStatus.GRADING_LOG_NOT_FOUND));

            mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}", "nonexistent_id")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4041"));

            then(queryGradingLogInputPort).should()
                    .getGradingLog(12345L, new GetGradingLogCommand("nonexistent_id"));
        }

        @Test
        @DisplayName("GRD-2-04: 다른 사용자의 gradingLogId로 조회 시 403")
        void getGradingLog_forbidden() throws Exception {
            given(queryGradingLogInputPort.getGradingLog(12345L, new GetGradingLogCommand("other-user-log-id")))
                    .willThrow(new CustomException(QuizErrorStatus.GRADING_ACCESS_DENIED));

            mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}", "other-user-log-id")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4031"));

            then(queryGradingLogInputPort).should()
                    .getGradingLog(12345L, new GetGradingLogCommand("other-user-log-id"));
        }

        @Test
        @DisplayName("GRD-2-05: JWT 토큰 없이 요청 시 401")
        void getGradingLog_unauthorized() throws Exception {
            mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}", "valid-log-id"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(queryGradingLogInputPort).shouldHaveNoInteractions();
        }
    }

    // =========================================================================
    // API 3: GET /v1/quiz/grading/{gradingLogId}/{quizId} — 채점 상세 조회
    // =========================================================================

    @Nested
    @DisplayName("API 3: GET /v1/quiz/grading/{gradingLogId}/{quizId} — 채점 상세 조회")
    class GetGradingDetailLog {

        @Test
        @DisplayName("GRD-3-01: 유효한 gradingLogId + quizId로 상세 조회 성공")
        void getGradingDetailLog_success() throws Exception {
            GradingDetailLogResult result = GradingDetailLogResult.builder()
                    .quizId(1L)
                    .score(100)
                    .isCorrect(true)
                    .content("TCP는 연결 지향 프로토콜인가?")
                    .quizType("OX")
                    .userAnswer("O")
                    .correctAnswer("O")
                    .feedback("정답입니다.")
                    .missingKeywords(List.of())
                    .improvedAnswer(null)
                    .build();

            given(queryGradingLogInputPort.getGradingDetailLog(12345L, new GetGradingDetailLogCommand("valid-log-id", 1L)))
                    .willReturn(result);

            mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}/{quizId}", "valid-log-id", 1)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.score").value(100))
                    .andExpect(jsonPath("$.data.isCorrect").value(true))
                    .andExpect(jsonPath("$.data.userAnswer").value("O"))
                    .andExpect(jsonPath("$.data.correctAnswer").value("O"))
                    .andExpect(jsonPath("$.data.feedback").value("정답입니다."));

            then(queryGradingLogInputPort).should()
                    .getGradingDetailLog(12345L, new GetGradingDetailLogCommand("valid-log-id", 1L));
        }

        @Test
        @DisplayName("GRD-3-03: 존재하지 않는 gradingLogId로 상세 조회 시 404")
        void getGradingDetailLog_logNotFound() throws Exception {
            given(queryGradingLogInputPort.getGradingDetailLog(12345L, new GetGradingDetailLogCommand("nonexistent_id", 1L)))
                    .willThrow(new CustomException(QuizErrorStatus.GRADING_LOG_NOT_FOUND));

            mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}/{quizId}", "nonexistent_id", 1)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4041"));

            then(queryGradingLogInputPort).should()
                    .getGradingDetailLog(12345L, new GetGradingDetailLogCommand("nonexistent_id", 1L));
        }

        @Test
        @DisplayName("GRD-3-04: 존재하지 않는 quizId로 상세 조회 시 404")
        void getGradingDetailLog_quizNotFound() throws Exception {
            given(queryGradingLogInputPort.getGradingDetailLog(12345L, new GetGradingDetailLogCommand("valid-log-id", 99999L)))
                    .willThrow(new CustomException(QuizErrorStatus.GRADING_RESULT_NOT_FOUND));

            mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}/{quizId}", "valid-log-id", 99999)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4042"));

            then(queryGradingLogInputPort).should()
                    .getGradingDetailLog(12345L, new GetGradingDetailLogCommand("valid-log-id", 99999L));
        }

        @Test
        @DisplayName("GRD-3-06: 다른 사용자의 채점 상세 조회 시 403")
        void getGradingDetailLog_forbidden() throws Exception {
            given(queryGradingLogInputPort.getGradingDetailLog(12345L, new GetGradingDetailLogCommand("other-user-log-id", 1L)))
                    .willThrow(new CustomException(QuizErrorStatus.GRADING_ACCESS_DENIED));

            mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}/{quizId}", "other-user-log-id", 1)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("GRADING4031"));

            then(queryGradingLogInputPort).should()
                    .getGradingDetailLog(12345L, new GetGradingDetailLogCommand("other-user-log-id", 1L));
        }

        @Test
        @DisplayName("GRD-3-07: JWT 토큰 없이 요청 시 401")
        void getGradingDetailLog_unauthorized() throws Exception {
            mockMvc.perform(get("/v1/quiz/grading/{gradingLogId}/{quizId}", "valid-log-id", 1))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(queryGradingLogInputPort).shouldHaveNoInteractions();
        }
    }

    // =========================================================================
    // API 4: GET /v1/quiz/grading/list — 채점 기록 목록 조회
    // =========================================================================

    @Nested
    @DisplayName("API 4: GET /v1/quiz/grading/list — 채점 기록 목록 조회")
    class GetGradingLogList {

        @Test
        @DisplayName("GRD-4-01: 채점 기록 목록 조회 성공")
        void getGradingLogList_success() throws Exception {
            List<GradingLogListResult> results = List.of(
                    new GradingLogListResult(
                            "log-001",
                            LocalDateTime.of(2026, 3, 29, 10, 30),
                            3, 5, "DAILY",
                            List.of("OS", "Network")
                    ),
                    new GradingLogListResult(
                            "log-002",
                            LocalDateTime.of(2026, 3, 28, 14, 0),
                            5, 5, "TOPIC",
                            List.of("Database")
                    )
            );

            given(queryGradingLogInputPort.getGradingLogList(12345L))
                    .willReturn(results);

            mockMvc.perform(get("/v1/quiz/grading/list")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value("log-001"))
                    .andExpect(jsonPath("$.data[0].solvedAt").exists())
                    .andExpect(jsonPath("$.data[0].correctCount").value(3))
                    .andExpect(jsonPath("$.data[0].totalCount").value(5))
                    .andExpect(jsonPath("$.data[0].quizMode").value("DAILY"))
                    .andExpect(jsonPath("$.data[0].topicNames").isArray())
                    .andExpect(jsonPath("$.data[0].topicNames[0]").value("OS"));

            then(queryGradingLogInputPort).should().getGradingLogList(12345L);
        }

        @Test
        @DisplayName("GRD-4-02: 채점 기록이 없는 경우 빈 리스트 반환")
        void getGradingLogList_emptyList() throws Exception {
            given(queryGradingLogInputPort.getGradingLogList(12345L))
                    .willReturn(List.of());

            mockMvc.perform(get("/v1/quiz/grading/list")
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            then(queryGradingLogInputPort).should().getGradingLogList(12345L);
        }

        @Test
        @DisplayName("GRD-4-04: JWT 토큰 없이 요청 시 401")
        void getGradingLogList_unauthorized() throws Exception {
            mockMvc.perform(get("/v1/quiz/grading/list"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(queryGradingLogInputPort).shouldHaveNoInteractions();
        }
    }
}
