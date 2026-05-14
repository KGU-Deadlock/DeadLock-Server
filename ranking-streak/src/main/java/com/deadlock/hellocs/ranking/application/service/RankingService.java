package com.deadlock.hellocs.ranking.application.service;

import com.deadlock.hellocs.ranking.application.port.in.QueryRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.UpdateRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.dto.MyRankingResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingEntryResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingQueryType;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.UpdateRankingCommand;
import com.deadlock.hellocs.ranking.application.port.out.LoadRankingPort;
import com.deadlock.hellocs.ranking.application.port.out.LoadUserInterestPort;
import com.deadlock.hellocs.ranking.application.port.out.LoadUserProfilePort;
import com.deadlock.hellocs.ranking.application.port.out.SaveRankingPort;
import com.deadlock.hellocs.ranking.domain.Ranking;
import com.deadlock.hellocs.ranking.domain.RankingKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService implements UpdateRankingUseCase, QueryRankingUseCase {

    private static final long FIRST_RANK = 1L;

    private final SaveRankingPort saveRankingPort;
    private final LoadRankingPort loadRankingPort;
    private final LoadUserInterestPort loadUserInterestPort;
    private final LoadUserProfilePort loadUserProfilePort;

    @Override
    public void update(UpdateRankingCommand command) {
        saveRankingPort.incrementScore(RankingKey.total(), command.userId(), command.score());
        command.topicIds().forEach(topicId ->
                saveRankingPort.incrementScore(RankingKey.topic(topicId), command.userId(), command.score())
        );
    }

    @Override
    public RankingSummaryResult getSummary(int size) {
        List<Ranking> top = loadRankingPort.loadByRankRange(RankingKey.total(), FIRST_RANK, size);
        return new RankingSummaryResult(enrichEntries(top), 0);
    }

    @Override
    public RankingDetailResult getRankingByType(Long userId, RankingQueryType type, int size) {
        RankingKey key = resolveKey(userId, type);
        List<RankingEntryResult> rankings = enrichEntries(
                loadRankingPort.loadByRankRange(key, FIRST_RANK, size)
        );

        Ranking myRankData = loadRankingPort.loadUserRank(key, userId);
        LoadUserProfilePort.UserProfile myProfile = loadUserProfilePort.loadProfile(userId);

        if (myRankData == null) {
            return new RankingDetailResult(
                    type.name(), rankings,
                    toMyRankingResult(myProfile, null, 0L),
                    List.of(), 0
            );
        }

        List<RankingEntryResult> belowMyRankings = enrichEntries(
                loadRankingPort.loadByRankRange(key, myRankData.rank() + 1, myRankData.rank() + 2)
        );

        return new RankingDetailResult(
                type.name(), rankings,
                toMyRankingResult(myProfile, myRankData.rank(), myRankData.score()),
                belowMyRankings, 0
        );
    }

    private RankingKey resolveKey(Long userId, RankingQueryType type) {
        return switch (type) {
            case ALL -> RankingKey.total();
            case INTEREST -> RankingKey.topic(
                    loadUserInterestPort.loadFirstInterestTopicId(userId).orElseThrow()
            );
        };
    }

    private List<RankingEntryResult> enrichEntries(List<Ranking> rankings) {
        if (rankings.isEmpty()) return List.of();
        List<Long> userIds = rankings.stream().map(Ranking::userId).toList();
        Map<Long, LoadUserProfilePort.UserProfile> profileMap = loadUserProfilePort.loadProfiles(userIds)
                .stream()
                .collect(Collectors.toMap(LoadUserProfilePort.UserProfile::userId, Function.identity()));
        return rankings.stream()
                .map(r -> toEntryResult(r, profileMap.get(r.userId())))
                .toList();
    }

    private RankingEntryResult toEntryResult(Ranking r, LoadUserProfilePort.UserProfile profile) {
        return new RankingEntryResult(
                r.rank(),
                r.userId(),
                profile != null ? profile.nickname() : null,
                profile != null ? profile.profileImage() : null,
                profile != null ? profile.interests() : List.of(),
                r.score()
        );
    }

    private MyRankingResult toMyRankingResult(LoadUserProfilePort.UserProfile profile, Long rank, long score) {
        return new MyRankingResult(
                profile.userId(),
                profile.nickname(),
                profile.profileImage(),
                profile.interests(),
                rank,
                score
        );
    }
}
