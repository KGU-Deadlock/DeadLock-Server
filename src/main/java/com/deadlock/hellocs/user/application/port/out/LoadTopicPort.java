package com.deadlock.hellocs.user.application.port.out;

import java.util.List;

public interface LoadTopicPort {
    List<String> getTopicNamesByIds(List<Long> topicIds);
    List<Long> getTopicIdsByNames(List<String> topicNames);
}
