package com.deadlock.hellocs.topic.application.port.in;

import com.deadlock.hellocs.topic.application.port.in.dto.TopicResult;

import java.util.List;
import java.util.Map;

public interface LoadTopicUseCase {
    List<String> getTopicNames(List<Long> ids);
    List<Long> getTopicIdsByNames(List<String> names);
    List<TopicResult> getAllTopics();
}
