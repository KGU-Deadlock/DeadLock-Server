package com.deadlock.hellocs.topic.application.port.out;

import com.deadlock.hellocs.topic.domain.Topic;
import java.util.List;

public interface LoadTopicPort {
    List<Topic> loadTopics(List<Long> ids);
    List<Topic> loadTopicsByNames(List<String> names);
    List<Topic> loadAllTopics();
}
