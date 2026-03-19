package com.deadlock.hellocs.streak.adapter.in.web;

import com.deadlock.hellocs.global.auth.handler.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.quiz.exception.QuizExceptionHandler;
import com.deadlock.hellocs.streak.application.port.in.QueryStreakInputPort;
import com.deadlock.hellocs.streak.application.port.in.dto.DailyStreakRecordResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StreakController.class)
@Import({SecurityConfig.class, QuizExceptionHandler.class})
@ActiveProfiles("test")
class StreakControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryStreakInputPort queryStreakInputPort;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("GET /v1/streak - 인증된 사용자는 스트릭 요약을 조회할 수 있다")
    void getSummary_success() throws Exception {
        given(queryStreakInputPort.getSummary(12345L))
                .willReturn(new StreakSummaryResult(4, 87, 5));

        mockMvc.perform(get("/v1/streak")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.currentStreakDays").value(4))
                .andExpect(jsonPath("$.data.solvedQuizCount").value(87))
                .andExpect(jsonPath("$.data.solvedTopicCount").value(5));

        then(queryStreakInputPort).should().getSummary(12345L);
    }

    @Test
    @DisplayName("GET /v1/streak?year&month - 월간 스트릭 기록을 조회할 수 있다")
    void getMonthly_success() throws Exception {
        given(queryStreakInputPort.getMonthly(12345L, 2025, 12))
                .willReturn(new StreakMonthlyResult(
                        2025,
                        12,
                        List.of(
                                new DailyStreakRecordResult("2025-12-01", true, 3, 1),
                                new DailyStreakRecordResult("2025-12-02", false, 0, 0)
                        )
                ));

        mockMvc.perform(get("/v1/streak")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .param("year", "2025")
                        .param("month", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.year").value(2025))
                .andExpect(jsonPath("$.data.month").value(12))
                .andExpect(jsonPath("$.data.days[0].date").value("2025-12-01"))
                .andExpect(jsonPath("$.data.days[0].solved").value(true));

        then(queryStreakInputPort).should().getMonthly(12345L, 2025, 12);
    }

    @Test
    @DisplayName("GET /v1/streak/detail - 스트릭 상세를 조회할 수 있다")
    void getDetail_success() throws Exception {
        given(queryStreakInputPort.getDetail(12345L))
                .willReturn(new StreakDetailResult(
                        4,
                        87,
                        5,
                        12,
                        LocalDate.of(2025, 12, 10),
                        true,
                        10,
                        23
                ));

        mockMvc.perform(get("/v1/streak/detail")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.longestStreakDays").value(12))
                .andExpect(jsonPath("$.data.lastSolvedDate").value("2025-12-10"))
                .andExpect(jsonPath("$.data.solvedToday").value(true));

        then(queryStreakInputPort).should().getDetail(12345L);
    }

    @Test
    @DisplayName("GET /v1/streak - 인증되지 않은 요청이면 401을 반환한다")
    void getSummary_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/streak"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON401"))
                .andExpect(jsonPath("$.data").value((Object) null));

        then(queryStreakInputPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /v1/streak?year&month - month가 범위를 벗어나면 400을 반환한다")
    void getMonthly_invalidMonth() throws Exception {
        mockMvc.perform(get("/v1/streak")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .param("year", "2025")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400"));

        then(queryStreakInputPort).shouldHaveNoInteractions();
    }
}
