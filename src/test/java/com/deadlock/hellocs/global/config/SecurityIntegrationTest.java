package com.deadlock.hellocs.global.config;

import com.deadlock.hellocs.ranking.application.port.in.QueryRankingInputPort;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
import com.deadlock.hellocs.streak.application.port.in.QueryStreakInputPort;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryRankingInputPort queryRankingInputPort;

    @MockBean
    private QueryStreakInputPort queryStreakInputPort;

    @Test
    void rankingSummaryPermitAll() throws Exception {
        when(queryRankingInputPort.getSummary()).thenReturn(
                new RankingSummaryResult(List.of(), 0)
        );

        mockMvc.perform(get("/api/v1/ranking/summary"))
                .andExpect(status().isOk());
    }

    @Test
    void rankingRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/ranking"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streakRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/streak"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtAllowsAccess() throws Exception {
        when(queryStreakInputPort.getSummary(1L)).thenReturn(
                new StreakSummaryResult(1, 1, 1)
        );

        mockMvc.perform(get("/api/v1/streak")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("1"))))
                .andExpect(status().isOk());
    }
}
