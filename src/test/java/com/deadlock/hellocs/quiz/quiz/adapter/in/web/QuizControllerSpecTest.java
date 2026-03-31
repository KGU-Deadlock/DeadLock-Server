package com.deadlock.hellocs.quiz.quiz.adapter.in.web;

import com.deadlock.hellocs.global.auth.handler.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.global.exception.GlobalExceptionHandler;
import com.deadlock.hellocs.quiz.exception.QuizErrorStatus;
import com.deadlock.hellocs.quiz.exception.QuizExceptionHandler;
import com.deadlock.hellocs.quiz.quiz.application.port.in.QueryQuizInputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.MultipleChoiceQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.OxQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.ShortAnswerQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.VoiceQuizResult;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, QuizExceptionHandler.class})
@ActiveProfiles("test")
class QuizControllerSpecTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryQuizInputPort queryQuizInputPort;

    @MockitoBean
    private LoadUserUseCase loadUserUseCase;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String QUIZ_URL = "/v1/quiz";

    @Nested
    @DisplayName("API 1: POST /v1/quiz — 퀴즈 조회 (정상)")
    class GetQuizzes_Success {

        @Test
        @DisplayName("QIZ-1-01: 유효한 topicIds(1개)와 mode=STANDARD로 퀴즈 조회 성공")
        void getQuizzes_standard_singleTopic_success() throws Exception {
            given(loadUserUseCase.getUserLevel(12345L)).willReturn(QuizLevel.JUNIOR);
            given(queryQuizInputPort.getQuizzes(any(GetQuizCommand.class), eq(12345L)))
                    .willReturn(new GetQuizResult(
                            List.of(new OxQuizResult(1L, "OX 문제")),
                            List.of(new MultipleChoiceQuizResult(2L, "객관식 문제", List.of("A", "B", "C"))),
                            List.of(new ShortAnswerQuizResult(3L, "단답형 문제")),
                            List.of()
                    ));

            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[1],\"mode\":\"STANDARD\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.oxQuizzes").isArray())
                    .andExpect(jsonPath("$.data.multipleChoiceQuizzes").isArray())
                    .andExpect(jsonPath("$.data.shortAnswerQuizzes").isArray());

            then(queryQuizInputPort).should().getQuizzes(any(GetQuizCommand.class), eq(12345L));
        }

        @Test
        @DisplayName("QIZ-1-02: mode=STANDARD, topicIds(2개 이상)로 퀴즈 조회 성공")
        void getQuizzes_standard_multipleTopics_success() throws Exception {
            given(loadUserUseCase.getUserLevel(12345L)).willReturn(QuizLevel.JUNIOR);
            given(queryQuizInputPort.getQuizzes(any(GetQuizCommand.class), eq(12345L)))
                    .willReturn(new GetQuizResult(
                            List.of(new OxQuizResult(1L, "OX 문제1"), new OxQuizResult(4L, "OX 문제2")),
                            List.of(new MultipleChoiceQuizResult(2L, "객관식 문제", List.of("A", "B"))),
                            List.of(new ShortAnswerQuizResult(3L, "단답형 문제")),
                            List.of()
                    ));

            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[1,2,3],\"mode\":\"STANDARD\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.oxQuizzes").isArray())
                    .andExpect(jsonPath("$.data.multipleChoiceQuizzes").isArray())
                    .andExpect(jsonPath("$.data.shortAnswerQuizzes").isArray());

            then(queryQuizInputPort).should().getQuizzes(any(GetQuizCommand.class), eq(12345L));
        }

        @Test
        @DisplayName("QIZ-1-03: mode=VOICE로 퀴즈 조회 성공")
        void getQuizzes_voice_singleTopic_success() throws Exception {
            given(loadUserUseCase.getUserLevel(12345L)).willReturn(QuizLevel.JUNIOR);
            given(queryQuizInputPort.getQuizzes(any(GetQuizCommand.class), eq(12345L)))
                    .willReturn(new GetQuizResult(
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(new VoiceQuizResult(1L, "음성 문제", "음성 텍스트"))
                    ));

            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[1],\"mode\":\"VOICE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.voiceQuizzes").isArray())
                    .andExpect(jsonPath("$.data.voiceQuizzes[0].id").value(1));

            then(queryQuizInputPort).should().getQuizzes(any(GetQuizCommand.class), eq(12345L));
        }

        @Test
        @DisplayName("QIZ-1-04: mode=VOICE, topicIds(2개 이상)로 퀴즈 조회 성공")
        void getQuizzes_voice_multipleTopics_success() throws Exception {
            given(loadUserUseCase.getUserLevel(12345L)).willReturn(QuizLevel.JUNIOR);
            given(queryQuizInputPort.getQuizzes(any(GetQuizCommand.class), eq(12345L)))
                    .willReturn(new GetQuizResult(
                            List.of(new OxQuizResult(1L, "OX 문제")),
                            List.of(new MultipleChoiceQuizResult(2L, "객관식 문제", List.of("A", "B"))),
                            List.of(new ShortAnswerQuizResult(3L, "단답형 문제")),
                            List.of(new VoiceQuizResult(4L, "음성 문제", "음성 텍스트"))
                    ));

            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[1,2,3],\"mode\":\"VOICE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.oxQuizzes").isArray())
                    .andExpect(jsonPath("$.data.multipleChoiceQuizzes").isArray())
                    .andExpect(jsonPath("$.data.shortAnswerQuizzes").isArray())
                    .andExpect(jsonPath("$.data.voiceQuizzes").isArray());

            then(queryQuizInputPort).should().getQuizzes(any(GetQuizCommand.class), eq(12345L));
        }
    }

    @Nested
    @DisplayName("API 1: POST /v1/quiz — 퀴즈 조회 (검증)")
    class GetQuizzes_Validation {

        @Test
        @DisplayName("QIZ-1-05: topicIds가 빈 리스트인 경우")
        void getQuizzes_emptyTopicIds_badRequest() throws Exception {
            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[],\"mode\":\"STANDARD\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("QIZ-1-06: topicIds가 null인 경우")
        void getQuizzes_nullTopicIds_badRequest() throws Exception {
            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":null,\"mode\":\"STANDARD\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("QIZ-1-07: topicIds에 null 항목 포함")
        void getQuizzes_topicIdsContainsNull_badRequest() throws Exception {
            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[1,null],\"mode\":\"STANDARD\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("QIZ-1-08: mode가 null인 경우")
        void getQuizzes_nullMode_badRequest() throws Exception {
            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[1],\"mode\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("QIZ-1-09: 유효하지 않은 mode 값")
        void getQuizzes_invalidMode_badRequest() throws Exception {
            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[1],\"mode\":\"INVALID\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
            //TODO: 잘못된 topic id에 대한 예외 처리가 구현되어 있지 않아 500 에러나 나옴.
        }

        @Test
        @DisplayName("QIZ-1-10: 존재하지 않는 topicId 포함")
        void getQuizzes_nonExistentTopicId_badRequest() throws Exception {
            given(loadUserUseCase.getUserLevel(12345L)).willReturn(QuizLevel.JUNIOR);
            given(queryQuizInputPort.getQuizzes(any(GetQuizCommand.class), eq(12345L)))
                    .willThrow(new CustomException(QuizErrorStatus.QUIZ_REQUEST_INVALID));

            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt.subject("12345")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[99999],\"mode\":\"STANDARD\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("API 1: POST /v1/quiz — 퀴즈 조회 (인증)")
    class GetQuizzes_Auth {

        @Test
        @DisplayName("QIZ-1-11: JWT 토큰 없이 요청")
        void getQuizzes_withoutJwt_unauthorized() throws Exception {
            mockMvc.perform(post(QUIZ_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[1],\"mode\":\"STANDARD\"}"))
                    .andExpect(status().isUnauthorized());
        }

        /*
        mockMvc 로 만료된 토큰의 테스트가 불가능
        @Test
        @DisplayName("QIZ-1-12: 만료된 JWT 토큰으로 요청")
        void getQuizzes_expiredJwt_unauthorized() throws Exception {
            mockMvc.perform(post(QUIZ_URL)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt
                                            .subject("12345")
                                            .expiresAt(java.time.Instant.now().minusSeconds(3600))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"topicIds\":[1],\"mode\":\"STANDARD\"}"))
                    .andExpect(status().isUnauthorized());
        }

         */
    }
}
