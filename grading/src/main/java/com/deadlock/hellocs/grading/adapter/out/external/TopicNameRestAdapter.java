package com.deadlock.hellocs.grading.adapter.out.external;

import com.deadlock.hellocs.grading.application.port.out.QueryTopicOutputPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * topic-service에서 토픽 이름 목록을 조회하는 REST 어댑터.
 * GET /v1/topics/names?ids=1,2,3 → List<String> 반환.
 */
@Component
public class TopicNameRestAdapter implements QueryTopicOutputPort {

    private final RestClient restClient;

    public TopicNameRestAdapter(@Value("${service.topic.url}") String topicServiceUrl) {
        this.restClient = RestClient.create(topicServiceUrl);
    }

    @Override
    public List<String> getTopicNames(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        NamesResponse resp = restClient.get()
                .uri(uri -> uri.path("/v1/topics/names")
                        .queryParam("ids", ids.toArray())
                        .build())
                .retrieve()
                .body(NamesResponse.class);
        return (resp != null && resp.data() != null) ? resp.data() : List.of();
    }

    private record NamesResponse(List<String> data) {}
}
