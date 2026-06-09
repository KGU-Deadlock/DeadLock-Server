package com.deadlock.hellocs.topic.adapter.out.persistence;

import com.deadlock.hellocs.topic.adapter.out.persistence.entity.TopicJpaEntity;
import com.deadlock.hellocs.topic.application.port.out.LoadTopicPort;
import com.deadlock.hellocs.topic.domain.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TopicPersistenceAdapter implements LoadTopicPort {
    private final TopicRepository topicRepository;

    @Override
    public List<Topic> loadTopics(List<Long> ids) {
        return topicRepository.findAllById(ids).stream()
                .map(TopicJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Topic> loadTopicsByNames(List<String> names) {
        return topicRepository.findByNameIn(names).stream()
                .map(TopicJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Topic> loadAllTopics() {
        return topicRepository.findAll().stream()
                .map(TopicJpaEntity::toDomain)
                .collect(Collectors.toList());
    }
}
