package com.deadlock.hellocs.global.client;

import com.deadlock.hellocs.global.client.port.TopicNamePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * core-service의 /internal/v1/topics 엔드포인트를 호출하는 어댑터.
 */
@Component
@RequiredArgsConstructor
public class CoreServiceTopicAdapter implements TopicNamePort {

    private final RestClient restClient;

    @Value("${core-service.base-url}")
    private String coreServiceBaseUrl;

    @Override
    public List<String> getTopicNamesByIds(List<Long> topicIds) {
        String ids = topicIds.stream().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse("");
        return restClient.get()
                .uri(coreServiceBaseUrl + "/internal/v1/topics/names?topicIds=" + ids)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public Map<Long, String> getTopicNameMapByIds(List<Long> topicIds) {
        String ids = topicIds.stream().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse("");
        return restClient.get()
                .uri(coreServiceBaseUrl + "/internal/v1/topics/name-map?topicIds=" + ids)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
