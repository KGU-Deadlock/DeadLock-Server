package com.deadlock.hellocs.ranking.adapter.out.user;

import com.deadlock.hellocs.ranking.application.port.out.LoadUserProfilePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * user-service REST API를 호출하여 사용자 프로필을 조회하는 어댑터.
 */
@Component
public class UserProfileRestAdapter implements LoadUserProfilePort {

    private final RestClient restClient;

    public UserProfileRestAdapter(@Value("${service.user.url}") String userServiceUrl) {
        this.restClient = RestClient.create(userServiceUrl);
    }

    @Override
    public UserProfile loadProfile(Long userId) {
        SingleWrapper resp = restClient.get()
                .uri("/v1/users/{userId}/profile-summary", userId)
                .retrieve()
                .body(SingleWrapper.class);
        if (resp == null || resp.data() == null) {
            return new UserProfile(userId, null, null, List.of());
        }
        ProfileData d = resp.data();
        return new UserProfile(d.userId(), d.nickname(), d.profileImage(), d.interests());
    }

    @Override
    public List<UserProfile> loadProfiles(List<Long> userIds) {
        if (userIds.isEmpty()) return List.of();
        ListWrapper resp = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/users/profile-summaries")
                        .queryParam("kakaoIds", userIds.toArray())
                        .build())
                .retrieve()
                .body(ListWrapper.class);
        if (resp == null || resp.data() == null) return List.of();
        return resp.data().stream()
                .map(d -> new UserProfile(d.userId(), d.nickname(), d.profileImage(), d.interests()))
                .toList();
    }

    private record ProfileData(Long userId, String nickname, String profileImage, List<String> interests) {}
    private record SingleWrapper(ProfileData data) {}
    private record ListWrapper(List<ProfileData> data) {}
}
