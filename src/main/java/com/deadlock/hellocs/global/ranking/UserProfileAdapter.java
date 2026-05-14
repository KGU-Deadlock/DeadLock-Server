package com.deadlock.hellocs.global.ranking;

import com.deadlock.hellocs.ranking.application.port.out.LoadUserProfilePort;
import com.deadlock.hellocs.user.application.port.in.LoadUserSummaryUseCase;
import com.deadlock.hellocs.user.application.port.in.UserSummaryResult;
import com.deadlock.hellocs.user.application.port.out.LoadTopicPort;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserProfileAdapter implements LoadUserProfilePort {

    private final LoadUserSummaryUseCase loadUserSummaryUseCase;
    private final LoadUserPort loadUserPort;
    private final LoadTopicPort loadTopicPort;

    @Override
    public UserProfile loadProfile(Long userId) {
        UserSummaryResult summary = loadUserSummaryUseCase.getUserSummary(userId);
        User user = loadUserPort.loadUserByKakaoId(userId);
        List<String> interests = resolveInterestNames(user.getInterestTopicIds());
        return new UserProfile(userId, summary.nickname(), summary.profileImage(), interests);
    }

    @Override
    public List<UserProfile> loadProfiles(List<Long> userIds) {
        if (userIds.isEmpty()) return List.of();

        Map<Long, UserSummaryResult> summaryMap = loadUserSummaryUseCase.getUserSummaries(userIds)
                .stream()
                .collect(Collectors.toMap(UserSummaryResult::kakaoId, Function.identity()));

        List<User> users = loadUserPort.loadUsersByKakaoIds(userIds);

        List<Long> allTopicIds = users.stream()
                .flatMap(u -> u.getInterestTopicIds().stream())
                .distinct()
                .toList();
        Map<Long, String> topicNameMap = allTopicIds.isEmpty()
                ? Map.of()
                : loadTopicPort.getTopicNameMapByIds(allTopicIds);

        Map<Long, List<String>> interestMap = users.stream()
                .collect(Collectors.toMap(
                        User::getKakaoId,
                        u -> u.getInterestTopicIds().stream()
                                .map(topicNameMap::get)
                                .filter(Objects::nonNull)
                                .toList()
                ));

        return userIds.stream()
                .map(id -> {
                    UserSummaryResult s = summaryMap.get(id);
                    return new UserProfile(
                            id,
                            s != null ? s.nickname() : null,
                            s != null ? s.profileImage() : null,
                            interestMap.getOrDefault(id, List.of())
                    );
                })
                .toList();
    }

    private List<String> resolveInterestNames(List<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) return List.of();
        Map<Long, String> nameMap = loadTopicPort.getTopicNameMapByIds(topicIds);
        return topicIds.stream().map(nameMap::get).filter(Objects::nonNull).toList();
    }
}
