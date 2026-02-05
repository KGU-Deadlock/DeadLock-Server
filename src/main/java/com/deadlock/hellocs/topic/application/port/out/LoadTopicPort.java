package com.deadlock.hellocs.topic.application.port.out;

import com.deadlock.hellocs.topic.domain.Topic;
import java.util.List;
import java.util.Optional;

public interface LoadTopicPort {
    Optional<Topic> loadTopic(Long id);
    List<Topic> loadTopics(List<Long> ids);
    List<Topic> loadTopicsByNames(List<String> names);
}
