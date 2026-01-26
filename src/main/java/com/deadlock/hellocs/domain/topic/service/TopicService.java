package com.deadlock.hellocs.domain.topic.service;

import com.deadlock.hellocs.domain.topic.entity.Topic;
import com.deadlock.hellocs.domain.topic.repository.TopicRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {
    private final TopicRepository topicRepository;

    /**
     * Retrieve the name of the topic identified by the given id.
     *
     * @param id the unique identifier of the topic
     * @return the topic's name, or {@code null} if no topic exists with the given id
     */
    public String GetTopicName(Long id) {
        Topic topic = topicRepository.findById(id).orElse(null);
        if (topic == null) {
            return null;
        }
        return topic.getName();
    }

    /**
     * Retrieves the names of topics for the provided topic IDs.
     *
     * @param ids the list of topic IDs to fetch; IDs with no corresponding topic are ignored
     * @return a list containing the names of the topics found for the given IDs; missing topics are omitted
     */
    public List<String> GetTopicNames(List<Long> ids) {
        return topicRepository.findAllById(ids).stream()
                .map(Topic::getName)
                .collect(Collectors.toList());
    }
}