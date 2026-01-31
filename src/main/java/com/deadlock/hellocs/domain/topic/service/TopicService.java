package com.deadlock.hellocs.domain.topic.service;

import com.deadlock.hellocs.domain.topic.entity.Topic;
import com.deadlock.hellocs.domain.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {
    private final TopicRepository topicRepository;

    public String getTopicName(Long id) {
        Topic topic = topicRepository.findById(id).orElse(null);
        if (topic == null) {
            return null;
        }
        return topic.getName();
    }

    public List<String> getTopicNames(List<Long> ids) {
        return topicRepository.findAllById(ids).stream()
                .map(Topic::getName)
                .collect(Collectors.toList());
    }
}
