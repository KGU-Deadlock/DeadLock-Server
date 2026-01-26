package com.deadlock.hellocs.domain.topic.repository;

import com.deadlock.hellocs.domain.topic.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
}
