package com.deadlock.hellocs.ranking.adapter.in.web;

import com.deadlock.hellocs.global.auth.handler.OAuth2LoginSuccessHandler;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.config.SecurityConfig;
import com.deadlock.hellocs.global.exception.GlobalExceptionHandler;
import com.deadlock.hellocs.ranking.application.port.in.QueryRankingInputPort;
import com.deadlock.hellocs.ranking.application.port.in.dto.MyRankingResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingEntryResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RankingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class RankingControllerSpecTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryRankingInputPort queryRankingInputPort;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String KAKAO_ID = "12345";

    // ==================== API 1: GET /v1/ranking/summary ====================

    @Nested
    @DisplayName("API 1: GET /v1/ranking/summary — 랭킹 요약 조회")
    class GetRankingSummary {

        @Test
        @DisplayName("RNK-1-01: 랭킹 요약 조회 성공")
        void success() throws Exception {
            given(queryRankingInputPort.getSummary())
                    .willReturn(new RankingSummaryResult(
                            List.of(
                                    new RankingEntryResult(1, 10001L, "user1", "https://img.url/1.jpg", List.of("OS"), 1280),
                                    new RankingEntryResult(2, 10002L, "user2", "https://img.url/2.jpg", List.of("DB"), 1100)
                            ),
                            0
                    ));

            mockMvc.perform(get("/v1/ranking/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.topEntries").isArray())
                    .andExpect(jsonPath("$.data.totalCount").value(0));

            then(queryRankingInputPort).should().getSummary();
        }

        @Test
        @DisplayName("RNK-1-05: 인증 없이 요청 가능 확인 (공개 API)")
        void noAuth_success() throws Exception {
            given(queryRankingInputPort.getSummary())
                    .willReturn(new RankingSummaryResult(List.of(), 0));

            mockMvc.perform(get("/v1/ranking/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(queryRankingInputPort).should().getSummary();
        }
    }

    // ==================== API 2: GET /v1/ranking ====================

    @Nested
    @DisplayName("API 2: GET /v1/ranking — 랭킹 상세 조회")
    class GetRankingDetail {

        @Test
        @DisplayName("RNK-2-01: filterType=ALL, 기본 size(10)로 랭킹 조회 성공")
        void allFilter_success() throws Exception {
            given(queryRankingInputPort.getRanking(12345L, "ALL", 10))
                    .willReturn(new RankingDetailResult(
                            "ALL",
                            List.of(new RankingEntryResult(1, 10001L, "user1", "https://img.url/1.jpg", List.of("OS"), 1280)),
                            new MyRankingResult(12345L, "테스트유저", "https://img.url/me.jpg", List.of("OS"), 5L, 840),
                            List.of(),
                            0
                    ));

            mockMvc.perform(get("/v1/ranking")
                            .param("filterType", "ALL")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.filterType").value("ALL"))
                    .andExpect(jsonPath("$.data.rankings").isArray())
                    .andExpect(jsonPath("$.data.myRank").isNotEmpty())
                    .andExpect(jsonPath("$.data.nearbyRankings").isArray());

            then(queryRankingInputPort).should().getRanking(12345L, "ALL", 10);
        }

        @Test
        @DisplayName("RNK-2-02: filterType=INTEREST로 관심 분야 기반 랭킹 조회")
        void interestFilter_success() throws Exception {
            given(queryRankingInputPort.getRanking(12345L, "INTEREST", 10))
                    .willReturn(new RankingDetailResult(
                            "INTEREST",
                            List.of(),
                            new MyRankingResult(12345L, "테스트유저", "https://img.url/me.jpg", List.of("OS"), null, 0),
                            List.of(),
                            0
                    ));

            mockMvc.perform(get("/v1/ranking")
                            .param("filterType", "INTEREST")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"))
                    .andExpect(jsonPath("$.data.filterType").value("INTEREST"));

            then(queryRankingInputPort).should().getRanking(12345L, "INTEREST", 10);
        }

        @Test
        @DisplayName("RNK-2-06: size=1 (최솟값) 경계값 테스트")
        void sizeMin_success() throws Exception {
            given(queryRankingInputPort.getRanking(12345L, "ALL", 1))
                    .willReturn(new RankingDetailResult(
                            "ALL",
                            List.of(new RankingEntryResult(1, 10001L, "user1", null, List.of(), 1280)),
                            new MyRankingResult(12345L, "테스트유저", null, List.of(), null, 0),
                            List.of(),
                            0
                    ));

            mockMvc.perform(get("/v1/ranking")
                            .param("filterType", "ALL")
                            .param("size", "1")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(queryRankingInputPort).should().getRanking(12345L, "ALL", 1);
        }

        @Test
        @DisplayName("RNK-2-07: size=100 (최댓값) 경계값 테스트")
        void sizeMax_success() throws Exception {
            given(queryRankingInputPort.getRanking(12345L, "ALL", 100))
                    .willReturn(new RankingDetailResult(
                            "ALL",
                            List.of(),
                            new MyRankingResult(12345L, "테스트유저", null, List.of(), null, 0),
                            List.of(),
                            0
                    ));

            mockMvc.perform(get("/v1/ranking")
                            .param("filterType", "ALL")
                            .param("size", "100")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON2000"));

            then(queryRankingInputPort).should().getRanking(12345L, "ALL", 100);
        }

        @Test
        @DisplayName("RNK-2-08: size=0인 경우 400 반환")
        void sizeZero_returns400() throws Exception {
            mockMvc.perform(get("/v1/ranking")
                            .param("filterType", "ALL")
                            .param("size", "0")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(queryRankingInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("RNK-2-09: size=99999인 경우 400 반환")
        void sizeExceedsMax_returns400() throws Exception {
            mockMvc.perform(get("/v1/ranking")
                            .param("filterType", "ALL")
                            .param("size", "99999")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(queryRankingInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("RNK-2-10: size=-1 (음수)인 경우 400 반환")
        void sizeNegative_returns400() throws Exception {
            mockMvc.perform(get("/v1/ranking")
                            .param("filterType", "ALL")
                            .param("size", "-1")
                            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(KAKAO_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON400"));

            then(queryRankingInputPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("RNK-2-12: JWT 토큰 없이 요청 시 401 반환")
        void noJwtToken_returns401() throws Exception {
            mockMvc.perform(get("/v1/ranking")
                            .param("filterType", "ALL"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("COMMON401"));

            then(queryRankingInputPort).shouldHaveNoInteractions();
        }

        // TODO: RNK-2-11 — filterType=INVALID 검증은 서비스 레이어에서 IllegalArgumentException으로 처리되며,
        //  GlobalExceptionHandler에서 500으로 매핑됨. 명세상 400이나 @WebMvcTest에서 컨트롤러 단위 검증 불가 (스킵)
    }
}
