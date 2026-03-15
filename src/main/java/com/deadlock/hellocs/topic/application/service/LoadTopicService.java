package com.deadlock.hellocs.topic.application.service;

import com.deadlock.hellocs.topic.application.port.in.LoadTopicUseCase;
import com.deadlock.hellocs.topic.application.port.in.dto.TopicResult;
import com.deadlock.hellocs.topic.application.port.out.LoadTopicPort;
import com.deadlock.hellocs.topic.domain.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoadTopicService implements LoadTopicUseCase {
    private final LoadTopicPort loadTopicPort;

    @Override
    public String getTopicName(Long id) {
        return loadTopicPort.loadTopic(id)
                .map(Topic::getName)
                .orElse(null);
    }

    @Override
    public List<String> getTopicNames(List<Long> ids) {
        return loadTopicPort.loadTopics(ids).stream()
                .map(Topic::getName)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getTopicIdsByNames(List<String> names) {
        return loadTopicPort.loadTopicsByNames(names).stream()
                .map(Topic::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<TopicResult> getAllTopics() {
        return loadTopicPort.loadAllTopics().stream()
                .map(TopicResult::from)
                .collect(Collectors.toList());
    }
}
