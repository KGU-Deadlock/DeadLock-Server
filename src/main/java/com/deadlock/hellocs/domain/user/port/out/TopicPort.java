package com.deadlock.hellocs.domain.user.port.out;

import java.util.List;

public interface TopicPort {
    List<String> getTopicNamesByIds(List<Long> topicIds);
}
