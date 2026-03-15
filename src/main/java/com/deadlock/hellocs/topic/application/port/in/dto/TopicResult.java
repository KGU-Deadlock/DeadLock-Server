package com.deadlock.hellocs.topic.application.port.in.dto;

import com.deadlock.hellocs.topic.domain.Topic;

public record TopicResult(Long id, String name) {
    public static TopicResult from(Topic topic) {
        return new TopicResult(topic.getId(), topic.getName());
    }
}
