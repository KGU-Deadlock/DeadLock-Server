package com.deadlock.hellocs.quiz.adapter.out.external;

import com.deadlock.hellocs.quiz.application.port.out.QuerySolvedQuizIdsOutputPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Set;

@Component
public class SolvedQuizIdsRestAdapter implements QuerySolvedQuizIdsOutputPort {

    private final RestClient restClient;

    public SolvedQuizIdsRestAdapter(@Value("${service.grading.url}") String gradingServiceUrl) {
        this.restClient = RestClient.create(gradingServiceUrl);
    }

    @Override
    public Set<Long> findByUserId(Long userId) {
        SolvedIdsResponse resp = restClient.get()
                .uri("/v1/gradings/users/{userId}/solved-quiz-ids", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<SolvedIdsResponse>() {});
        return (resp != null && resp.data() != null) ? resp.data() : Set.of();
    }

    private record SolvedIdsResponse(boolean isSuccess, Set<Long> data) {}
}
