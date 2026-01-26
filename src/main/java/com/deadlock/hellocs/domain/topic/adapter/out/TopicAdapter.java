package com.deadlock.hellocs.domain.topic.adapter.out;

import com.deadlock.hellocs.domain.topic.service.TopicService;
import com.deadlock.hellocs.domain.user.port.out.TopicPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TopicAdapter implements TopicPort {

    private final TopicService topicService;

    /**
     * Retrieves the topic names for the given topic IDs.
     *
     * @param topicIds list of topic identifiers to resolve to names
     * @return a list of topic names corresponding to the provided IDs
     */
    @Override
    public List<String> getTopicNamesByIds(List<Long> topicIds) {
        return topicService.GetTopicNames(topicIds);
    }
}