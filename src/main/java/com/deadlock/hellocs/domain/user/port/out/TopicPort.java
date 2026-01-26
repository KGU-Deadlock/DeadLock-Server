package com.deadlock.hellocs.domain.user.port.out;

import java.util.List;

public interface TopicPort {
    /**
 * Resolve topic names for the given topic IDs.
 *
 * @param topicIds the list of topic IDs to resolve; may be empty
 * @return a list of topic names where each element is the name of the topic at the same index in {@code topicIds}
 */
List<String> getTopicNamesByIds(List<Long> topicIds);
}