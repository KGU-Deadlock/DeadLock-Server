package com.deadlock.hellocs.ranking.application.service;

import com.deadlock.hellocs.ranking.application.port.in.ApplyRankingScoreInputPort;
import com.deadlock.hellocs.ranking.application.port.in.QueryRankingInputPort;
import com.deadlock.hellocs.ranking.application.port.in.dto.ApplyRankingScoreCommand;
import com.deadlock.hellocs.ranking.application.port.in.dto.MyRankingResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingEntryResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
import com.deadlock.hellocs.ranking.application.port.out.LoadRankingPort;
import com.deadlock.hellocs.ranking.application.port.out.UpdateRankingPort;
import com.deadlock.hellocs.ranking.domain.RankingEntry;
import com.deadlock.hellocs.user.application.port.in.LoadUserSummaryUseCase;
import com.deadlock.hellocs.user.application.port.in.UserSummaryResult;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
public class RankingService implements ApplyRankingScoreInputPort, QueryRankingInputPort {

    private final UpdateRankingPort updateRankingPort;
    private final LoadRankingPort loadRankingPort;
    private final LoadUserSummaryUseCase loadUserSummaryUseCase;
    private final LoadUserPort loadUserPort;

    @Override
    public void apply(@Valid ApplyRankingScoreCommand command) {
        // 수정 표시
        updateRankingPort.increaseScoreIfAbsent(
                command.gradingLogId(),
                command.kakaoId(),
                command.totalScore(),
                command.topicIds()
        );
    }

    @Override
    public RankingSummaryResult getSummary() {
        // 수정 표시
        List<RankingEntryResult> top5 = mapRankingEntries(loadRankingPort.loadTopRankings(5));
        return new RankingSummaryResult(top5, 0);
    }

    @Override
    public RankingDetailResult getRanking(
            @NotNull Long kakaoId,
            @NotNull String filterType,
            @Min(1) @Max(100) int size
    ) {
        // 수정 표시
        String normalizedFilterType = filterType.trim().toUpperCase();
        UserSummaryResult userSummary = loadUserSummaryUseCase.getUserSummary(kakaoId);

        if ("INTEREST".equals(normalizedFilterType)) {
            return buildInterestRanking(userSummary, size);
        }
        if (!"ALL".equals(normalizedFilterType)) {
            throw new IllegalArgumentException("Unsupported filterType: " + filterType);
        }
        return buildGlobalRanking(userSummary, size);
    }

    private RankingDetailResult buildGlobalRanking(UserSummaryResult userSummary, int size) {
        // 수정 표시
        List<RankingEntry> rankings = loadRankingPort.loadTopRankings(size);
        RankingEntry myEntry = loadRankingPort.loadRanking(userSummary.kakaoId())
                .orElse(null);
        List<RankingEntry> belowMyRankings = myEntry != null
                ? loadRankingPort.loadRankingsByRankRange(myEntry.rank() + 1L, myEntry.rank() + 2L)
                : List.of();

        return new RankingDetailResult(
                "ALL",
                mapRankingEntries(rankings),
                toMyRankingResult(myEntry, userSummary),
                mapRankingEntries(belowMyRankings),
                0
        );
    }

    private RankingDetailResult buildInterestRanking(UserSummaryResult userSummary, int size) {
        // 수정 표시
        User user = loadUserPort.loadUserByKakaoId(userSummary.kakaoId());
        Map<Long, Long> mergedScores = new HashMap<>();

        for (Long topicId : user.getInterestTopicIds()) {
            loadRankingPort.loadTopicScores(topicId).forEach(
                    (memberKakaoId, score) -> mergedScores.merge(memberKakaoId, score, Long::sum)
            );
        }

        if (mergedScores.isEmpty()) {
            return new RankingDetailResult(
                    "INTEREST",
                    List.of(),
                    toMyRankingResult(null, userSummary),
                    List.of(),
                    0
            );
        }

        List<Long> orderedIds = mergedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();
        List<RankingEntry> allEntries = toRankedEntries(orderedIds, mergedScores);

        RankingEntry myEntry = allEntries.stream()
                .filter(entry -> entry.kakaoId().equals(userSummary.kakaoId()))
                .findFirst()
                .orElse(null);
        long myRank = myEntry != null ? myEntry.rank() : 0L;

        List<RankingEntry> belowMyRankings = myRank > 0
                ? allEntries.stream()
                .filter(entry -> entry.rank() > myRank && entry.rank() <= myRank + 2L)
                .toList()
                : List.of();

        return new RankingDetailResult(
                "INTEREST",
                mapRankingEntries(allEntries.stream().limit(size).toList()),
                toMyRankingResult(myEntry, userSummary),
                mapRankingEntries(belowMyRankings),
                0
        );
    }

    private List<RankingEntry> toRankedEntries(List<Long> orderedIds, Map<Long, Long> mergedScores) {
        // 수정 표시
        return java.util.stream.IntStream.range(0, orderedIds.size())
                .mapToObj(index -> new RankingEntry(
                        orderedIds.get(index),
                        mergedScores.getOrDefault(orderedIds.get(index), 0L),
                        index + 1L
                ))
                .toList();
    }

    private MyRankingResult toMyRankingResult(RankingEntry entry, UserSummaryResult userSummary) {
        if (entry == null) {
            return new MyRankingResult(
                    userSummary.kakaoId(),
                    userSummary.nickname(),
                    userSummary.profileImage(),
                    null,
                    0L
            );
        }

        return new MyRankingResult(
                userSummary.kakaoId(),
                userSummary.nickname(),
                userSummary.profileImage(),
                entry.rank(),
                entry.score()
        );
    }

    private List<RankingEntryResult> mapRankingEntries(List<RankingEntry> rankings) {
        if (rankings.isEmpty()) {
            return List.of();
        }

        Map<Long, UserSummaryResult> userSummaryMap = loadUserSummaryUseCase.getUserSummaries(
                        rankings.stream().map(RankingEntry::kakaoId).toList())
                .stream()
                .collect(Collectors.toMap(UserSummaryResult::kakaoId, Function.identity()));

        return rankings.stream()
                .map(entry -> toRankingEntryResult(entry, userSummaryMap.get(entry.kakaoId())))
                .toList();
    }

    private RankingEntryResult toRankingEntryResult(RankingEntry entry, UserSummaryResult userSummary) {
        return new RankingEntryResult(
                entry.rank(),
                entry.kakaoId(),
                userSummary != null ? userSummary.nickname() : null,
                userSummary != null ? userSummary.profileImage() : null,
                entry.score()
        );
    }
}
