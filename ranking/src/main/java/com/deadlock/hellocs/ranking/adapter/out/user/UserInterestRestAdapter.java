package com.deadlock.hellocs.ranking.adapter.out.user;

import com.deadlock.hellocs.ranking.application.port.out.LoadUserInterestPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * user-service REST API를 호출하여 사용자 관심 주제 ID를 조회하는 어댑터.
 */
@Component
public class UserInterestRestAdapter implements LoadUserInterestPort {

    private final RestClient restClient;

    public UserInterestRestAdapter(@Value("${service.user.url}") String userServiceUrl) {
        this.restClient = RestClient.create(userServiceUrl);
    }

    @Override
    public Optional<Long> loadFirstInterestTopicId(Long userId) {
        InterestResponse response = restClient.get()
                .uri("/v1/users/{userId}/interest-topic", userId)
                .retrieve()
                .body(InterestResponse.class);
        return Optional.ofNullable(response != null ? response.data() : null);
    }

    private record InterestResponse(Long data) {}
}
