package com.deadlock.hellocs.global.ranking;

import com.deadlock.hellocs.ranking.application.port.out.LoadUserInterestPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class UserInterestAdapter implements LoadUserInterestPort {

    private final RestClient restClient;

    public UserInterestAdapter(@Value("${server.port}") int port) {
        this.restClient = RestClient.create("http://localhost:" + port);
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
