package com.deadlock.hellocs.streak.adapter.in.web;

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
    private JwtDecoder jwtDecoder;

    private static final String KAKAO_ID = "12345";

    @Nested
    @DisplayName("GET /v1/streak — 스트릭 요약")
    class GetStreakSummary {

        @Test
        @DisplayName("STK-1-01: 스트릭 요약 조회 성공")
        void success() throws Exception {
            given(queryStreakInputPort.getSummary(12345L)).willReturn(new StreakSummaryResult(4, 87, 5));

            mockMvc.perform(get("/v1/streak")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.data.currentStreakDays").value(4))
                    .andExpect(jsonPath("$.data.solvedQuizCount").value(87))
                    .andExpect(jsonPath("$.data.solvedTopicCount").value(5));

            then(queryStreakInputPort).should().getSummary(12345L);
        }

        @Test
        @DisplayName("STK-1-02: 신규 사용자 — 모든 값 0")
        void newUser_allZero() throws Exception {
            given(queryStreakInputPort.getSummary(12345L)).willReturn(new StreakSummaryResult(0, 0, 0));

            mockMvc.perform(get("/v1/streak")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentStreakDays").value(0));
        }

        @Test
        @DisplayName("STK-1-04: JWT 없으면 401")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(get("/v1/streak"))
                    .andExpect(status().isUnauthorized());
            then(queryStreakInputPort).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("GET /v1/streak?year=&month= — 월별 스트릭")
    class GetMonthlyStreak {

        @Test
        @DisplayName("STK-2-01: 유효한 연/월로 조회 성공")
        void success() throws Exception {
            given(queryStreakInputPort.getMonthly(12345L, 2026, 3)).willReturn(
                    new StreakMonthlyResult(2026, 3,
                            List.of(new DailyStreakRecordResult("2026-03-01", true, 3, 1))));

            mockMvc.perform(get("/v1/streak").param("year", "2026").param("month", "3")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.year").value(2026))
                    .andExpect(jsonPath("$.data.month").value(3))
                    .andExpect(jsonPath("$.data.days").isArray());
        }

        @Test
        @DisplayName("STK-2-03: year < 2000 → 400")
        void yearBelow2000_returns400() throws Exception {
            mockMvc.perform(get("/v1/streak").param("year", "1999").param("month", "3")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(KAKAO_ID))))
                    .andExpect(status().isBadRequest());
            then(queryStreakInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("STK-2-04: month=0 → 400")
        void monthZero_returns400() throws Exception {
            mockMvc.perform(get("/v1/streak").param("year", "2026").param("month", "0")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(KAKAO_ID))))
                    .andExpect(status().isBadRequest());
            then(queryStreakInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("STK-2-05: month=13 → 400")
        void monthOver12_returns400() throws Exception {
            mockMvc.perform(get("/v1/streak").param("year", "2026").param("month", "13")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(KAKAO_ID))))
                    .andExpect(status().isBadRequest());
            then(queryStreakInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("STK-2-08: JWT 없으면 401")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(get("/v1/streak").param("year", "2026").param("month", "3"))
                    .andExpect(status().isUnauthorized());
            then(queryStreakInputPort).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("GET /v1/streak/detail — 스트릭 상세")
    class GetStreakDetail {

        @Test
        @DisplayName("STK-3-01: 스트릭 상세 조회 성공")
        void success() throws Exception {
            given(queryStreakInputPort.getDetail(12345L)).willReturn(
                    new StreakDetailResult(4, 87, 5, 12, LocalDate.of(2026, 3, 31), true, 10, 23));

            mockMvc.perform(get("/v1/streak/detail")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentStreakDays").value(4))
                    .andExpect(jsonPath("$.data.longestStreakDays").value(12))
                    .andExpect(jsonPath("$.data.solvedToday").value(true))
                    .andExpect(jsonPath("$.data.lastSolvedDate").value("2026-03-31"));
        }

        @Test
        @DisplayName("STK-3-02: 신규 사용자 — 기본값 확인")
        void newUser_defaultValues() throws Exception {
            given(queryStreakInputPort.getDetail(12345L))
                    .willReturn(new StreakDetailResult(0, 0, 0, 0, null, false, 0, 0));

            mockMvc.perform(get("/v1/streak/detail")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentStreakDays").value(0))
                    .andExpect(jsonPath("$.data.solvedToday").value(false));
        }

        @Test
        @DisplayName("STK-3-05: JWT 없으면 401")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(get("/v1/streak/detail"))
                    .andExpect(status().isUnauthorized());
            then(queryStreakInputPort).shouldHaveNoInteractions();
        }
    }
}
