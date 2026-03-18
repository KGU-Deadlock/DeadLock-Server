package com.deadlock.hellocs.ranking.adapter.in.web;

import com.deadlock.hellocs.global.config.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.quiz.exception.QuizExceptionHandler;
import com.deadlock.hellocs.ranking.application.port.in.QueryRankingInputPort;
import com.deadlock.hellocs.ranking.application.port.in.dto.MyRankingResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingEntryResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RankingController.class)
@Import({SecurityConfig.class, QuizExceptionHandler.class})
@ActiveProfiles("test")
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryRankingInputPort queryRankingInputPort;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("GET /api/v1/ranking/summary - 공개 요약 랭킹을 조회할 수 있다")
    void getRankingSummary_success() throws Exception {
        RankingSummaryResult result = new RankingSummaryResult(
                List.of(
                        new RankingEntryResult(1L, 10001L, "cs_runner", "https://cdn.example.com/1.png", 1280L),
                        new RankingEntryResult(2L, 10002L, "algo_fox", null, 1170L)
                ),
                0
        );

        given(queryRankingInputPort.getSummary()).willReturn(result);

        mockMvc.perform(get("/api/v1/ranking/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.top5[0].rank").value(1))
                .andExpect(jsonPath("$.data.top5[0].nickname").value("cs_runner"))
                .andExpect(jsonPath("$.data.recentRelatedDiscussionCount").value(0));

        then(queryRankingInputPort).should().getSummary();
    }

    @Test
    @DisplayName("GET /api/v1/ranking - 인증된 사용자는 실시간 랭킹 상세를 조회할 수 있다")
    void getRanking_success() throws Exception {
        RankingDetailResult result = new RankingDetailResult(
                "ALL",
                List.of(
                        new RankingEntryResult(1L, 10001L, "cs_runner", "https://cdn.example.com/1.png", 1280L),
                        new RankingEntryResult(2L, 12345L, "me", null, 1200L)
                ),
                new MyRankingResult(12345L, "me", null, 2L, 1200L),
                List.of(
                        new RankingEntryResult(3L, 10003L, "db_master", null, 1180L),
                        new RankingEntryResult(4L, 10004L, "os_ninja", null, 1100L)
                ),
                0
        );

        given(queryRankingInputPort.getRanking(12345L, "ALL", 10)).willReturn(result);

        mockMvc.perform(get("/api/v1/ranking")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .param("filterType", "ALL")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON2000"))
                .andExpect(jsonPath("$.data.filterType").value("ALL"))
                .andExpect(jsonPath("$.data.rankings[1].nickname").value("me"))
                .andExpect(jsonPath("$.data.myRanking.rank").value(2))
                .andExpect(jsonPath("$.data.belowMyRankings[0].nickname").value("db_master"));

        then(queryRankingInputPort).should().getRanking(12345L, "ALL", 10);
    }

    @Test
    @DisplayName("GET /api/v1/ranking - 인증되지 않은 요청이면 401을 반환한다")
    void getRanking_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/ranking")
                        .param("filterType", "ALL")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON401"))
                .andExpect(jsonPath("$.data").value((Object) null));

        then(queryRankingInputPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GET /api/v1/ranking - size가 범위를 벗어나면 400을 반환한다")
    void getRanking_invalidSize() throws Exception {
        mockMvc.perform(get("/api/v1/ranking")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("12345")))
                        .param("filterType", "ALL")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400"));

        then(queryRankingInputPort).shouldHaveNoInteractions();
    }
}
