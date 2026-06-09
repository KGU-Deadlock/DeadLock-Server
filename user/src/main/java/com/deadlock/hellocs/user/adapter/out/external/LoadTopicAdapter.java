package com.deadlock.hellocs.user.adapter.out.external;

import com.deadlock.hellocs.user.application.port.out.LoadTopicPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * topic-service REST API를 호출하여 토픽 정보를 조회하는 어댑터.
 */
@Component
public class LoadTopicAdapter implements LoadTopicPort {

    private final RestClient restClient;

    public LoadTopicAdapter(@Value("${service.topic.url}") String topicServiceUrl) {
        this.restClient = RestClient.create(topicServiceUrl);
    }

    @Override
    public List<String> getTopicNamesByIds(List<Long> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) return List.of();
        NamesResponse resp = restClient.get()
                .uri(uri -> uri.path("/v1/topics/names")
                        .queryParam("ids", topicIds.toArray())
                        .build())
                .retrieve()
                .body(NamesResponse.class);
        return (resp != null && resp.data() != null) ? resp.data() : List.of();
    }

    @Override
    public Map<Long, String> getTopicNameMapByIds(List<Long> topicIds) {
        List<String> names = getTopicNamesByIds(topicIds);
        Map<Long, String> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < topicIds.size() && i < names.size(); i++) {
            result.put(topicIds.get(i), names.get(i));
        }
        return result;
    }

    @Override
    public List<Long> getTopicIdsByNames(List<String> topicNames) {
        if (topicNames == null || topicNames.isEmpty()) return List.of();
        IdsResponse resp = restClient.get()
                .uri(uri -> uri.path("/v1/topics/ids")
                        .queryParam("names", topicNames.toArray())
                        .build())
                .retrieve()
                .body(IdsResponse.class);
        return (resp != null && resp.data() != null) ? resp.data() : List.of();
    }

    private record NamesResponse(List<String> data) {}
    private record IdsResponse(List<Long> data) {}
}
