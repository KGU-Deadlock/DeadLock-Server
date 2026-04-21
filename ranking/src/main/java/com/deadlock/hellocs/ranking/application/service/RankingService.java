package com.deadlock.hellocs.ranking.application.service;

import com.deadlock.hellocs.ranking.application.port.in.QueryRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.UpdateRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingEntryResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingQueryType;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.UpdateRankingCommand;
import com.deadlock.hellocs.ranking.application.port.out.LoadRankingPort;
import com.deadlock.hellocs.ranking.application.port.out.LoadUserInterestPort;
import com.deadlock.hellocs.ranking.application.port.out.SaveRankingPort;
import com.deadlock.hellocs.ranking.domain.Ranking;
import com.deadlock.hellocs.ranking.domain.RankingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService implements UpdateRankingUseCase, QueryRankingUseCase {

    private static final long FIRST_RANK = 1L;

    private final SaveRankingPort saveRankingPort;
    private final LoadRankingPort loadRankingPort;
    private final LoadUserInterestPort loadUserInterestPort;

    @Override
    public void update(UpdateRankingCommand command) {
        saveRankingPort.incrementScore(RankingKey.total(), command.userId(), command.score());
        command.topicIds().forEach(topicId ->
                saveRankingPort.incrementScore(RankingKey.topic(topicId), command.userId(), command.score())
        );
    }

    @Override
    public RankingSummaryResult getSummary(int size) {
        List<RankingEntryResult> topEntries = loadRankRange(RankingKey.total(), FIRST_RANK, size);
        long total = loadRankingPort.countTotal();
        return new RankingSummaryResult(topEntries, total);
    }

    @Override
    public RankingDetailResult getRankingByType(Long userId, RankingQueryType type, int size) {
        RankingKey key = resolveKey(userId, type);
        List<RankingEntryResult> rankings = loadRankRange(key, FIRST_RANK, size);
        RankingEntryResult myRank = loadMyRank(key, userId);
        if (myRank == null) {
            return new RankingDetailResult(rankings, null, List.of());
        }
        List<RankingEntryResult> nearbyRankings = loadRankRange(key, myRank.rank() + 1, myRank.rank() + 2);
        return new RankingDetailResult(rankings, myRank, nearbyRankings);
    }

    private RankingKey resolveKey(Long userId, RankingQueryType type) {
        return switch (type) {
            case ALL -> RankingKey.total();
            case INTEREST -> RankingKey.topic(
                    loadUserInterestPort.loadFirstInterestTopicId(userId).orElseThrow()
            );
        };
    }

    private List<RankingEntryResult> loadRankRange(RankingKey key, long startRank, long endRank) {
        return loadRankingPort.loadByRankRange(key, startRank, endRank).stream()
                .map(r -> new RankingEntryResult(r.rank(), r.userId(), r.score()))
                .toList();
    }

    private RankingEntryResult loadMyRank(RankingKey key, Long userId) {
        Ranking myRankData = loadRankingPort.loadUserRank(key, userId);
        if (myRankData == null) return null;
        return new RankingEntryResult(myRankData.rank(), myRankData.userId(), myRankData.score());
    }

}