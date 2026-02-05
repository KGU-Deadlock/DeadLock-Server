package com.deadlock.hellocs.topic.adapter.out.persistence.entity;

import com.deadlock.hellocs.topic.domain.Topic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "topics")
public class TopicJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 20, nullable = false, unique = true)
    private String name;

    public Topic toDomain() {
        return Topic.builder()
                .id(this.id)
                .name(this.name)
                .build();
    }

    public static TopicJpaEntity from(Topic topic) {
        return TopicJpaEntity.builder()
                .id(topic.getId())
                .name(topic.getName())
                .build();
    }
}
