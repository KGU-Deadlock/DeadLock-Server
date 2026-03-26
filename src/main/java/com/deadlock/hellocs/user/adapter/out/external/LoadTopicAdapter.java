package com.deadlock.hellocs.user.adapter.out.external;

import com.deadlock.hellocs.topic.application.port.in.LoadTopicUseCase;
import com.deadlock.hellocs.user.application.port.out.LoadTopicPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoadTopicAdapter implements LoadTopicPort {
    private final LoadTopicUseCase loadTopicUseCase;

    @Override
    public List<String> getTopicNamesByIds(List<Long> topicIds) {
        return loadTopicUseCase.getTopicNames(topicIds);
    }

    @Override
    public Map<Long, String> getTopicNameMapByIds(List<Long> topicIds) {
        return loadTopicUseCase.getTopicNameMap(topicIds);
    }

    @Override
    public List<Long> getTopicIdsByNames(List<String> topicNames) {
        return loadTopicUseCase.getTopicIdsByNames(topicNames);
    }
}
