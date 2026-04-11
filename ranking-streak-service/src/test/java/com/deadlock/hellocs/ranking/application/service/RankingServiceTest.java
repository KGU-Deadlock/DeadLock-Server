package com.deadlock.hellocs.ranking.application.service;

import com.deadlock.hellocs.global.client.dto.UserDetail;
import com.deadlock.hellocs.global.client.dto.UserSummaryResult;
import com.deadlock.hellocs.global.client.port.TopicNamePort;
import com.deadlock.hellocs.global.client.port.UserDetailPort;
import com.deadlock.hellocs.global.client.port.UserSummaryPort;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
import com.deadlock.hellocs.ranking.application.port.out.LoadRankingPort;
import com.deadlock.hellocs.ranking.application.port.out.UpdateRankingPort;
import com.deadlock.hellocs.ranking.domain.RankingEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock private UpdateRankingPort updateRankingPort;
    @Mock private LoadRankingPort loadRankingPort;
    @Mock private UserSummaryPort userSummaryPort;
    @Mock private UserDetailPort userDetailPort;
    @Mock private TopicNamePort topicNamePort;

    @InjectMocks
    private RankingService rankingService;

    @Test
    @DisplayName("랭킹 요약 조회 - 관심 토픽 포함")
    void getSummary_includesTopUserInterests() {
        Long firstKakaoId = 10001L;
        Long secondKakaoId = 10002L;

        given(loadRankingPort.loadTopRankings(5)).willReturn(List.of(
                new RankingEntry(firstKakaoId, 1280L, 1L),
                new RankingEntry(secondKakaoId, 1170L, 2L)
        ));
        given(userSummaryPort.getUserSummaries(List.of(firstKakaoId, secondKakaoId))).willReturn(List.of(
                new UserSummaryResult(firstKakaoId, "cs_runner", "https://cdn.example.com/1.png"),
                new UserSummaryResult(secondKakaoId, "algo_fox", null)
        ));
        given(userDetailPort.getUserDetails(List.of(firstKakaoId, secondKakaoId))).willReturn(List.of(
                new UserDetail(firstKakaoId, List.of(10L, 20L)),
                new UserDetail(secondKakaoId, List.of(30L))
        ));
        given(topicNamePort.getTopicNameMapByIds(List.of(10L, 20L, 30L)))
                .willReturn(Map.of(10L, "OS", 20L, "Network", 30L, "Database"));

        RankingSummaryResult result = rankingService.getSummary();

        assertThat(result.top5()).hasSize(2);
        assertThat(result.top5().get(0).interests()).containsExactlyInAnyOrder("OS", "Network");
        assertThat(result.top5().get(1).interests()).containsExactly("Database");
    }

    @Test
    @DisplayName("전체 랭킹 조회 - 내 관심 토픽 포함")
    void getRanking_ALL_includesCurrentUserInterests() {
        Long kakaoId = 12345L;

        given(userSummaryPort.getUserSummary(kakaoId))
                .willReturn(new UserSummaryResult(kakaoId, "me", null));
        given(loadRankingPort.loadTopRankings(10))
                .willReturn(List.of(new RankingEntry(kakaoId, 1200L, 2L)));
        given(loadRankingPort.loadRanking(kakaoId))
                .willReturn(Optional.of(new RankingEntry(kakaoId, 1200L, 2L)));
        given(loadRankingPort.loadRankingsByRankRange(3L, 4L)).willReturn(List.of());
        given(userSummaryPort.getUserSummaries(List.of(kakaoId)))
                .willReturn(List.of(new UserSummaryResult(kakaoId, "me", null)));
        given(userDetailPort.getUserDetails(List.of(kakaoId)))
                .willReturn(List.of(new UserDetail(kakaoId, List.of(10L, 20L))));
        given(topicNamePort.getTopicNameMapByIds(List.of(10L, 20L)))
                .willReturn(Map.of(10L, "OS", 20L, "Network"));
        given(userDetailPort.getUserDetail(kakaoId))
                .willReturn(new UserDetail(kakaoId, List.of(10L, 20L)));
        given(topicNamePort.getTopicNamesByIds(List.of(10L, 20L)))
                .willReturn(List.of("OS", "Network"));

        RankingDetailResult result = rankingService.getRanking(kakaoId, "ALL", 10);

        assertThat(result.myRanking().interests()).containsExactly("OS", "Network");
    }

    @Test
    @DisplayName("관심 분야 랭킹 조회 - 합산 점수 기반 정렬")
    void getRanking_INTEREST_mergesTopicScores() {
        Long kakaoId = 12345L;

        given(userSummaryPort.getUserSummary(kakaoId))
                .willReturn(new UserSummaryResult(kakaoId, "me", null));
        given(userDetailPort.getUserDetail(kakaoId))
                .willReturn(new UserDetail(kakaoId, List.of(10L, 20L)));
        given(topicNamePort.getTopicNamesByIds(List.of(10L, 20L)))
                .willReturn(List.of("OS", "Network"));
        given(loadRankingPort.loadTopicScores(10L)).willReturn(Map.of(kakaoId, 700L));
        given(loadRankingPort.loadTopicScores(20L)).willReturn(Map.of(kakaoId, 500L));
        given(userSummaryPort.getUserSummaries(List.of(kakaoId)))
                .willReturn(List.of(new UserSummaryResult(kakaoId, "me", null)));
        given(userDetailPort.getUserDetails(List.of(kakaoId)))
                .willReturn(List.of(new UserDetail(kakaoId, List.of(10L, 20L))));
        given(topicNamePort.getTopicNameMapByIds(List.of(10L, 20L)))
                .willReturn(Map.of(10L, "OS", 20L, "Network"));

        RankingDetailResult result = rankingService.getRanking(kakaoId, "INTEREST", 10);

        assertThat(result.filterType()).isEqualTo("INTEREST");
        assertThat(result.myRanking().score()).isEqualTo(1200L); // 700 + 500
        assertThat(result.myRanking().interests()).containsExactly("OS", "Network");
    }
}
