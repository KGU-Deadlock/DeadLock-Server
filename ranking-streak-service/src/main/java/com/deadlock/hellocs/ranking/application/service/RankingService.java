package com.deadlock.hellocs.ranking.application.service;

import com.deadlock.hellocs.global.client.dto.UserDetail;
import com.deadlock.hellocs.global.client.dto.UserSummaryResult;
import com.deadlock.hellocs.global.client.port.TopicNamePort;
import com.deadlock.hellocs.global.client.port.UserDetailPort;
import com.deadlock.hellocs.global.client.port.UserSummaryPort;
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
import jakarta.validation.Valid;
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
    private final UserSummaryPort userSummaryPort;
    private final UserDetailPort userDetailPort;
    private final TopicNamePort topicNamePort;

    @Override
    public void apply(@Valid ApplyRankingScoreCommand command) {
        updateRankingPort.increaseScoreIfAbsent(
                command.gradingLogId(),
                command.kakaoId(),
                command.totalScore(),
                command.topicIds()
        );
    }

    @Override
    public RankingSummaryResult getSummary() {
        List<RankingEntryResult> top5 = mapRankingEntries(loadRankingPort.loadTopRankings(5));
        return new RankingSummaryResult(top5, 0);
    }

    @Override
    public RankingDetailResult getRanking(Long kakaoId, String filterType, int size) {
        String normalizedFilterType = filterType.trim().toUpperCase();
        UserSummaryResult userSummary = userSummaryPort.getUserSummary(kakaoId);

        if ("INTEREST".equals(normalizedFilterType)) {
            return buildInterestRanking(userSummary, size);
        }
        if (!"ALL".equals(normalizedFilterType)) {
            throw new IllegalArgumentException("Unsupported filterType: " + filterType);
        }
        return buildGlobalRanking(userSummary, size);
    }

    private RankingDetailResult buildGlobalRanking(UserSummaryResult userSummary, int size) {
        List<RankingEntry> rankings = loadRankingPort.loadTopRankings(size);
        RankingEntry myEntry = loadRankingPort.loadRanking(userSummary.kakaoId()).orElse(null);
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
        UserDetail userDetail = userDetailPort.getUserDetail(userSummary.kakaoId());
        Map<Long, Long> mergedScores = new HashMap<>();

        for (Long topicId : userDetail.interestTopicIds()) {
            loadRankingPort.loadTopicScores(topicId).forEach(
                    (memberKakaoId, score) -> mergedScores.merge(memberKakaoId, score, Long::sum)
            );
        }

        if (mergedScores.isEmpty()) {
            return new RankingDetailResult("INTEREST", List.of(),
                    toMyRankingResult(null, userSummary), List.of(), 0);
        }

        List<Long> orderedIds = mergedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();
        List<RankingEntry> allEntries = toRankedEntries(orderedIds, mergedScores);

        RankingEntry myEntry = allEntries.stream()
                .filter(e -> e.kakaoId().equals(userSummary.kakaoId()))
                .findFirst().orElse(null);
        long myRank = myEntry != null ? myEntry.rank() : 0L;

        List<RankingEntry> belowMyRankings = myRank > 0
                ? allEntries.stream()
                .filter(e -> e.rank() > myRank && e.rank() <= myRank + 2L)
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
        return java.util.stream.IntStream.range(0, orderedIds.size())
                .mapToObj(i -> new RankingEntry(
                        orderedIds.get(i),
                        mergedScores.getOrDefault(orderedIds.get(i), 0L),
                        i + 1L
                ))
                .toList();
    }

    private MyRankingResult toMyRankingResult(RankingEntry entry, UserSummaryResult userSummary) {
        List<String> interests = loadCurrentUserInterests(userSummary.kakaoId());
        if (entry == null) {
            return new MyRankingResult(userSummary.kakaoId(), userSummary.nickname(),
                    userSummary.profileImage(), interests, null, 0L);
        }
        return new MyRankingResult(userSummary.kakaoId(), userSummary.nickname(),
                userSummary.profileImage(), interests, entry.rank(), entry.score());
    }

    private List<String> loadCurrentUserInterests(Long kakaoId) {
        UserDetail userDetail = userDetailPort.getUserDetail(kakaoId);
        List<Long> interestTopicIds = userDetail.interestTopicIds();
        if (interestTopicIds == null || interestTopicIds.isEmpty()) {
            return List.of();
        }
        return topicNamePort.getTopicNamesByIds(interestTopicIds);
    }

    private List<RankingEntryResult> mapRankingEntries(List<RankingEntry> rankings) {
        if (rankings.isEmpty()) return List.of();

        List<Long> kakaoIds = rankings.stream().map(RankingEntry::kakaoId).toList();
        Map<Long, UserSummaryResult> userSummaryMap = userSummaryPort.getUserSummaries(kakaoIds)
                .stream().collect(Collectors.toMap(UserSummaryResult::kakaoId, Function.identity()));
        Map<Long, List<String>> interestMap = loadUserInterests(kakaoIds);

        return rankings.stream()
                .map(e -> toRankingEntryResult(e, userSummaryMap.get(e.kakaoId()),
                        interestMap.getOrDefault(e.kakaoId(), List.of())))
                .toList();
    }

    private Map<Long, List<String>> loadUserInterests(List<Long> kakaoIds) {
        List<UserDetail> userDetails = userDetailPort.getUserDetails(kakaoIds);
        Map<Long, List<Long>> userTopicIds = userDetails.stream()
                .collect(Collectors.toMap(
                        UserDetail::kakaoId,
                        d -> d.interestTopicIds() == null ? List.of() : d.interestTopicIds()
                ));

        List<Long> topicIds = userTopicIds.values().stream()
                .flatMap(List::stream).distinct().toList();
        if (topicIds.isEmpty()) {
            return kakaoIds.stream().collect(Collectors.toMap(Function.identity(), ignored -> List.of()));
        }

        Map<Long, String> topicNameMap = topicNamePort.getTopicNameMapByIds(topicIds);
        return kakaoIds.stream().collect(Collectors.toMap(
                Function.identity(),
                id -> userTopicIds.getOrDefault(id, List.of()).stream()
                        .map(topicNameMap::get)
                        .filter(java.util.Objects::nonNull)
                        .toList()
        ));
    }

    private RankingEntryResult toRankingEntryResult(RankingEntry entry, UserSummaryResult userSummary,
                                                     List<String> interests) {
        return new RankingEntryResult(
                entry.rank(),
                entry.kakaoId(),
                userSummary != null ? userSummary.nickname() : null,
                userSummary != null ? userSummary.profileImage() : null,
                interests,
                entry.score()
        );
    }
}
