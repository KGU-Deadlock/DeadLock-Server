package com.deadlock.hellocs.user.application.port.out;

import java.util.List;
import java.util.Map;

public interface LoadTopicPort {
    List<String> getTopicNamesByIds(List<Long> topicIds);
    Map<Long, String> getTopicNameMapByIds(List<Long> topicIds);
    List<Long> getTopicIdsByNames(List<String> topicNames);
}
