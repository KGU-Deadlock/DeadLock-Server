package com.deadlock.hellocs.topic.application.port.in;

import com.deadlock.hellocs.topic.application.port.in.dto.TopicResult;

import java.util.List;

public interface LoadTopicUseCase {
    String getTopicName(Long id);
    List<String> getTopicNames(List<Long> ids);
    List<Long> getTopicIdsByNames(List<String> names);
    List<TopicResult> getAllTopics();
}
