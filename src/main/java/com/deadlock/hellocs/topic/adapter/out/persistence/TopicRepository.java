package com.deadlock.hellocs.topic.adapter.out.persistence;

import com.deadlock.hellocs.topic.adapter.out.persistence.entity.TopicJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<TopicJpaEntity, Long> {
    List<TopicJpaEntity> findByNameIn(List<String> names);
}
