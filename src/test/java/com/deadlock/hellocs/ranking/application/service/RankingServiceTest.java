package com.deadlock.hellocs.ranking.application.service;

import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
import com.deadlock.hellocs.ranking.application.port.out.LoadRankingPort;
import com.deadlock.hellocs.ranking.application.port.out.UpdateRankingPort;
import com.deadlock.hellocs.ranking.domain.RankingEntry;
import com.deadlock.hellocs.user.application.port.in.LoadUserSummaryUseCase;
import com.deadlock.hellocs.user.application.port.in.UserSummaryResult;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private UpdateRankingPort updateRankingPort;

    @Mock
    private LoadRankingPort loadRankingPort;

    @Mock
    private LoadUserSummaryUseCase loadUserSummaryUseCase;

    @Mock
    private LoadUserPort loadUserPort;

    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new RankingService(updateRankingPort, loadRankingPort, loadUserSummaryUseCase, loadUserPort);
    }

    @Test
    void summaryReturnsTop5() {
        when(loadRankingPort.loadTopRankings(5)).thenReturn(List.of(
                new RankingEntry(100L, 320L, 1L),
                new RankingEntry(200L, 250L, 2L)
        ));
        when(loadUserSummaryUseCase.getUserSummaries(List.of(100L, 200L))).thenReturn(List.of(
                new UserSummaryResult(100L, "alpha", "img-a", List.of(1L)),
                new UserSummaryResult(200L, "beta", "img-b", List.of(2L))
        ));

        RankingSummaryResult result = rankingService.getSummary();

        assertEquals(2, result.top5().size());
        assertEquals("alpha", result.top5().get(0).nickname());
        assertEquals(320L, result.top5().get(0).score());
        assertEquals(0, result.recentRelatedDiscussionCount());
    }

    @Test
    void allRankingReturnsMyRankingAndBelowRankings() {
        when(loadUserSummaryUseCase.getUserSummary(100L))
                .thenReturn(new UserSummaryResult(100L, "alpha", "img-a", List.of(1L)));
        when(loadRankingPort.loadTopRankings(2)).thenReturn(List.of(
                new RankingEntry(100L, 320L, 1L),
                new RankingEntry(200L, 250L, 2L)
        ));
        when(loadRankingPort.loadRanking(100L)).thenReturn(Optional.of(new RankingEntry(100L, 320L, 1L)));
        when(loadRankingPort.loadRankingsByRankRange(2L, 3L)).thenReturn(List.of(
                new RankingEntry(200L, 250L, 2L),
                new RankingEntry(300L, 200L, 3L)
        ));
        when(loadUserSummaryUseCase.getUserSummaries(List.of(100L, 200L))).thenReturn(List.of(
                new UserSummaryResult(100L, "alpha", "img-a", List.of(1L)),
                new UserSummaryResult(200L, "beta", "img-b", List.of(2L))
        ));
        when(loadUserSummaryUseCase.getUserSummaries(List.of(200L, 300L))).thenReturn(List.of(
                new UserSummaryResult(200L, "beta", "img-b", List.of(2L)),
                new UserSummaryResult(300L, "gamma", "img-c", List.of(3L))
        ));

        RankingDetailResult result = rankingService.getRanking(100L, "ALL", 2);

        assertEquals("ALL", result.filterType());
        assertEquals(2, result.rankings().size());
        assertEquals(1L, result.myRanking().rank());
        assertEquals(2, result.belowMyRankings().size());
    }

    @Test
    void interestRankingMergesScores() {
        when(loadUserSummaryUseCase.getUserSummary(100L))
                .thenReturn(new UserSummaryResult(100L, "alpha", "img-a", List.of(1L, 2L)));
        when(loadUserPort.loadUserByKakaoId(100L)).thenReturn(User.builder()
                .kakaoId(100L)
                .interestTopicIds(List.of(1L, 2L))
                .build());
        when(loadRankingPort.loadTopicScores(1L)).thenReturn(Map.of(100L, 100L, 200L, 50L));
        when(loadRankingPort.loadTopicScores(2L)).thenReturn(Map.of(100L, 40L, 300L, 80L));
        when(loadUserSummaryUseCase.getUserSummaries(List.of(100L, 300L))).thenReturn(List.of(
                new UserSummaryResult(100L, "alpha", "img-a", List.of(1L, 2L)),
                new UserSummaryResult(300L, "gamma", "img-c", List.of(2L))
        ));
        when(loadUserSummaryUseCase.getUserSummaries(List.of(300L, 200L))).thenReturn(List.of(
                new UserSummaryResult(300L, "gamma", "img-c", List.of(2L)),
                new UserSummaryResult(200L, "beta", "img-b", List.of(1L))
        ));

        RankingDetailResult result = rankingService.getRanking(100L, "INTEREST", 2);

        assertEquals("INTEREST", result.filterType());
        assertEquals(1L, result.myRanking().rank());
        assertEquals(140L, result.myRanking().score());
        assertEquals(2, result.belowMyRankings().size());
    }

    @Test
    void myRankingNullWhenNotRanked() {
        when(loadUserSummaryUseCase.getUserSummary(100L))
                .thenReturn(new UserSummaryResult(100L, "alpha", "img-a", List.of()));
        when(loadRankingPort.loadTopRankings(1)).thenReturn(List.of());
        when(loadRankingPort.loadRanking(100L)).thenReturn(Optional.empty());

        RankingDetailResult result = rankingService.getRanking(100L, "ALL", 1);

        assertNull(result.myRanking().rank());
        assertEquals(0L, result.myRanking().score());
    }

    @Test
    void filterTypeThrowsException() {
        when(loadUserSummaryUseCase.getUserSummary(100L))
                .thenReturn(new UserSummaryResult(100L, "alpha", "img-a", List.of()));

        assertThrows(IllegalArgumentException.class,
                () -> rankingService.getRanking(100L, "INVALID", 1));
    }

    @Test
    void applyDelegatesToUpdatePort() {
        rankingService.apply(new com.deadlock.hellocs.ranking.application.port.in.dto.ApplyRankingScoreCommand(
                "log-1",
                100L,
                180,
                List.of(1L, 2L)
        ));

        verify(updateRankingPort).increaseScoreIfAbsent("log-1", 100L, 180, List.of(1L, 2L));
    }
}
