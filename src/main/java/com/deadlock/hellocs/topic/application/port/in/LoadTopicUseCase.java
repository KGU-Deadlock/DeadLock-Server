package com.deadlock.hellocs.topic.application.port.in;

import java.util.List;

public interface LoadTopicUseCase {
    String getTopicName(Long id);
    List<String> getTopicNames(List<Long> ids);
    List<Long> getTopicIdsByNames(List<String> names);
}
