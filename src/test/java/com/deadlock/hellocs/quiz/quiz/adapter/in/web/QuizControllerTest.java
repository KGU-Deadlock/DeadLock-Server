package com.deadlock.hellocs.quiz.quiz.adapter.in.web;

import com.deadlock.hellocs.global.auth.handler.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.config.SecurityConfig;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
@Import({SecurityConfig.class, QuizExceptionHandler.class})
@ActiveProfiles("test")
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryQuizInputPort queryQuizInputPort;

    @MockitoBean
    private LoadUserUseCase loadUserUseCase;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("POST /v1/quiz - 인증된 사용자는 퀴즈 목록을 조회할 수 있다")
    void getQuizzes_success() throws Exception {
        GetQuizResult result = new GetQuizResult(
                List.of(new OxQuizResult(1L, "OX question")),
                List.of(new MultipleChoiceQuizResult(2L, "MC question", List.of("A", "B", "C", "D"))),
                List.of(new ShortAnswerQuizResult(3L, "Short question")),
                List.of(new VoiceQuizResult(4L, "voice-url", "Voice question"))
        );

        given(loadUserUseCase.getUserLevel(12345L)).willReturn(QuizLevel.PRO);
        given(queryQuizInputPort.getQuizzes(eq(new GetQuizCommand(QuizLevel.PRO, List.of(10L, 20L), QuizMode.STANDARD))))
                .willReturn(result);

        mockMvc.perform(post("/v1/quiz")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicIds": [10, 20],
                                  "mode": "STANDARD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.oxQuizzes[0].id").value(1))
                .andExpect(jsonPath("$.data.multipleChoiceQuizzes[0].choices[2]").value("C"))
                .andExpect(jsonPath("$.data.shortAnswerQuizzes[0].content").value("Short question"))
                .andExpect(jsonPath("$.data.voiceQuizzes[0].contentText").value("Voice question"));

        then(loadUserUseCase).should().getUserLevel(12345L);
        then(queryQuizInputPort).should()
                .getQuizzes(new GetQuizCommand(QuizLevel.PRO, List.of(10L, 20L), QuizMode.STANDARD));
    }

    @Test
    @DisplayName("POST /v1/quiz - 잘못된 요청 본문이면 400을 반환한다")
    void getQuizzes_invalidRequest() throws Exception {
        mockMvc.perform(post("/v1/quiz")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicIds": [],
                                  "mode": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("QUIZ4001"))
                .andExpect(jsonPath("$.data").doesNotExist());

        then(loadUserUseCase).should(never()).getUserLevel(12345L);
        then(queryQuizInputPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /v1/quiz - 인증되지 않은 요청이면 401을 반환한다")
    void getQuizzes_unauthorized() throws Exception {
        mockMvc.perform(post("/v1/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicIds": [10],
                                  "mode": "STANDARD"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON401"))
                .andExpect(jsonPath("$.data").value((Object) null));

        then(loadUserUseCase).shouldHaveNoInteractions();
        then(queryQuizInputPort).shouldHaveNoInteractions();
    }
}
