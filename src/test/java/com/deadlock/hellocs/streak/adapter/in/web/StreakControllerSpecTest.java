package com.deadlock.hellocs.streak.adapter.in.web;

import com.deadlock.hellocs.global.auth.handler.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.global.exception.GlobalExceptionHandler;
import com.deadlock.hellocs.streak.application.port.in.QueryStreakInputPort;
import com.deadlock.hellocs.streak.application.port.in.dto.DailyStreakRecordResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class StreakControllerSpecTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryStreakInputPort queryStreakInputPort;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String KAKAO_ID = "12345";

    // ==================== API 1: GET /v1/streak ====================

    @Nested
    @DisplayName("API 1: GET /v1/streak — 스트릭 요약")
    class GetStreakSummary {

        @Test
        @DisplayName("STK-1-01: 스트릭 요약 조회 성공")
        void success() throws Exception {
            given(queryStreakInputPort.getSummary(12345L))
                    .willReturn(new StreakSummaryResult(4, 87, 5));

            mockMvc.perform(get("/v1/streak")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.currentStreakDays").value(4))
                    .andExpect(jsonPath("$.data.solvedQuizCount").value(87))
                    .andExpect(jsonPath("$.data.solvedTopicCount").value(5));

            then(queryStreakInputPort).should().getSummary(12345L);
        }

        @Test
        @DisplayName("STK-1-02: 신규 사용자 스트릭 요약 — 모든 값 0")
        void newUser_allZero() throws Exception {
            given(queryStreakInputPort.getSummary(12345L))
                    .willReturn(new StreakSummaryResult(0, 0, 0));

            mockMvc.perform(get("/v1/streak")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.currentStreakDays").value(0))
                    .andExpect(jsonPath("$.data.solvedQuizCount").value(0))
                    .andExpect(jsonPath("$.data.solvedTopicCount").value(0));

            then(queryStreakInputPort).should().getSummary(12345L);
        }

        @Test
        @DisplayName("STK-1-03: 퀴즈를 푼 사용자 스트릭 요약 — 값 1 이상")
        void activeUser_valuesAboveZero() throws Exception {
            given(queryStreakInputPort.getSummary(12345L))
                    .willReturn(new StreakSummaryResult(3, 15, 2));

            mockMvc.perform(get("/v1/streak")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.currentStreakDays").isNumber())
                    .andExpect(jsonPath("$.data.solvedQuizCount").isNumber())
                    .andExpect(jsonPath("$.data.solvedTopicCount").isNumber());

            then(queryStreakInputPort).should().getSummary(12345L);
        }

        @Test
        @DisplayName("STK-1-04: JWT 토큰 없이 요청 시 401 반환")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(get("/v1/streak"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(queryStreakInputPort).shouldHaveNoInteractions();
        }
    }

    // ==================== API 2: GET /v1/streak?year=&month= ====================

    @Nested
    @DisplayName("API 2: GET /v1/streak?year=&month= — 월별 스트릭")
    class GetMonthlyStreak {

        @Test
        @DisplayName("STK-2-01: 유효한 연/월로 월별 스트릭 조회 성공")
        void success() throws Exception {
            given(queryStreakInputPort.getMonthly(12345L, 2026, 3))
                    .willReturn(new StreakMonthlyResult(
                            2026, 3,
                            List.of(new DailyStreakRecordResult("2026-03-01", true, 3, 1))
                    ));

            mockMvc.perform(get("/v1/streak")
                            .param("year", "2026")
                            .param("month", "3")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.year").value(2026))
                    .andExpect(jsonPath("$.data.month").value(3))
                    .andExpect(jsonPath("$.data.days").isArray());

            then(queryStreakInputPort).should().getMonthly(12345L, 2026, 3);
        }

        @Test
        @DisplayName("STK-2-02: 활동 없는 월 조회 — 빈 days 리스트")
        void noActivity_emptyDays() throws Exception {
            given(queryStreakInputPort.getMonthly(12345L, 2025, 1))
                    .willReturn(new StreakMonthlyResult(2025, 1, List.of()));

            mockMvc.perform(get("/v1/streak")
                            .param("year", "2025")
                            .param("month", "1")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.days").isEmpty());

            then(queryStreakInputPort).should().getMonthly(12345L, 2025, 1);
        }

        @Test
        @DisplayName("STK-2-03: year가 2000 미만 시 400 반환")
        void yearBelow2000_returns400() throws Exception {
            mockMvc.perform(get("/v1/streak")
                            .param("year", "1999")
                            .param("month", "3")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(queryStreakInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("STK-2-04: month가 0인 경우 400 반환")
        void monthZero_returns400() throws Exception {
            mockMvc.perform(get("/v1/streak")
                            .param("year", "2026")
                            .param("month", "0")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(queryStreakInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("STK-2-05: month가 13인 경우 400 반환")
        void monthOver12_returns400() throws Exception {
            mockMvc.perform(get("/v1/streak")
                            .param("year", "2026")
                            .param("month", "13")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(queryStreakInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("STK-2-06: year=2000, month=1 (최솟값) 경계값 테스트")
        void minBoundary_success() throws Exception {
            given(queryStreakInputPort.getMonthly(12345L, 2000, 1))
                    .willReturn(new StreakMonthlyResult(2000, 1, List.of()));

            mockMvc.perform(get("/v1/streak")
                            .param("year", "2000")
                            .param("month", "1")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(queryStreakInputPort).should().getMonthly(12345L, 2000, 1);
        }

        @Test
        @DisplayName("STK-2-07: month=12 (최댓값) 경계값 테스트")
        void maxBoundary_success() throws Exception {
            given(queryStreakInputPort.getMonthly(12345L, 2026, 12))
                    .willReturn(new StreakMonthlyResult(2026, 12, List.of()));

            mockMvc.perform(get("/v1/streak")
                            .param("year", "2026")
                            .param("month", "12")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(queryStreakInputPort).should().getMonthly(12345L, 2026, 12);
        }

        @Test
        @DisplayName("STK-2-08: JWT 토큰 없이 요청 시 401 반환")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(get("/v1/streak")
                            .param("year", "2026")
                            .param("month", "3"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(queryStreakInputPort).shouldHaveNoInteractions();
        }
    }

    // ==================== API 3: GET /v1/streak/detail ====================

    @Nested
    @DisplayName("API 3: GET /v1/streak/detail — 스트릭 상세")
    class GetStreakDetail {

        @Test
        @DisplayName("STK-3-01: 스트릭 상세 조회 성공")
        void success() throws Exception {
            given(queryStreakInputPort.getDetail(12345L))
                    .willReturn(new StreakDetailResult(
                            4, 87, 5, 12,
                            LocalDate.of(2026, 3, 31), true, 10, 23
                    ));

            mockMvc.perform(get("/v1/streak/detail")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.currentStreakDays").value(4))
                    .andExpect(jsonPath("$.data.solvedQuizCount").value(87))
                    .andExpect(jsonPath("$.data.solvedTopicCount").value(5))
                    .andExpect(jsonPath("$.data.longestStreakDays").value(12))
                    .andExpect(jsonPath("$.data.lastSolvedDate").value("2026-03-31"))
                    .andExpect(jsonPath("$.data.solvedToday").value(true))
                    .andExpect(jsonPath("$.data.activeDaysThisMonth").value(10))
                    .andExpect(jsonPath("$.data.currentMonthSolvedQuizCount").value(23));

            then(queryStreakInputPort).should().getDetail(12345L);
        }

        @Test
        @DisplayName("STK-3-02: 신규 사용자 상세 — 기본값 확인")
        void newUser_defaultValues() throws Exception {
            given(queryStreakInputPort.getDetail(12345L))
                    .willReturn(new StreakDetailResult(
                            0, 0, 0, 0,
                            null, false, 0, 0
                    ));

            mockMvc.perform(get("/v1/streak/detail")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.currentStreakDays").value(0))
                    .andExpect(jsonPath("$.data.solvedQuizCount").value(0))
                    .andExpect(jsonPath("$.data.lastSolvedDate").isEmpty())
                    .andExpect(jsonPath("$.data.solvedToday").value(false));

            then(queryStreakInputPort).should().getDetail(12345L);
        }

        @Test
        @DisplayName("STK-3-03: 오늘 퀴즈를 푼 사용자 — solvedToday=true")
        void solvedToday_true() throws Exception {
            LocalDate today = LocalDate.now();
            given(queryStreakInputPort.getDetail(12345L))
                    .willReturn(new StreakDetailResult(
                            5, 30, 3, 10,
                            today, true, 8, 15
                    ));

            mockMvc.perform(get("/v1/streak/detail")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.solvedToday").value(true))
                    .andExpect(jsonPath("$.data.lastSolvedDate").value(today.toString()));

            then(queryStreakInputPort).should().getDetail(12345L);
        }

        @Test
        @DisplayName("STK-3-04: longestStreakDays >= currentStreakDays 관계 확인")
        void longestGreaterThanOrEqualCurrent() throws Exception {
            given(queryStreakInputPort.getDetail(12345L))
                    .willReturn(new StreakDetailResult(
                            4, 50, 3, 12,
                            LocalDate.of(2026, 3, 31), true, 10, 20
                    ));

            mockMvc.perform(get("/v1/streak/detail")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.currentStreakDays").value(4))
                    .andExpect(jsonPath("$.data.longestStreakDays").value(12));

            then(queryStreakInputPort).should().getDetail(12345L);
        }

        @Test
        @DisplayName("STK-3-05: JWT 토큰 없이 요청 시 401 반환")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(get("/v1/streak/detail"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(queryStreakInputPort).shouldHaveNoInteractions();
        }
    }
}
