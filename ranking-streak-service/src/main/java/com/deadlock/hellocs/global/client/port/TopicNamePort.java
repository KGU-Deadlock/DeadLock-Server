package com.deadlock.hellocs.global.client.port;

import java.util.List;
import java.util.Map;

/**
 * core-service의 user.LoadTopicPort를 대체하는 로컬 포트.
 */
public interface TopicNamePort {
    List<String> getTopicNamesByIds(List<Long> topicIds);
    Map<Long, String> getTopicNameMapByIds(List<Long> topicIds);
}
