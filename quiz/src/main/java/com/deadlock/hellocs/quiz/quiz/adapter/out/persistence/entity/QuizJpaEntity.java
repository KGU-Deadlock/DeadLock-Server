package com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity;

import com.deadlock.hellocs.global.entity.BaseJpaEntity;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz JPA Entity
 * 
 * Joined 전략으로 상속 구현
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quiz SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Table(name = "quiz")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
public abstract class QuizJpaEntity extends BaseJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private QuizLevel level;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", insertable = false, updatable = false)
    private QuizType type;
    
    @ElementCollection
    @CollectionTable(
            name = "quiz_category",
            joinColumns = @JoinColumn(name = "quiz_id")
    )
    @Column(name = "topic_id")
    @lombok.Builder.Default
    private List<Long> topicIds = new ArrayList<>();
    
    /**
     * Entity → Domain 변환
     */
    public abstract Quiz toDomain();
}
