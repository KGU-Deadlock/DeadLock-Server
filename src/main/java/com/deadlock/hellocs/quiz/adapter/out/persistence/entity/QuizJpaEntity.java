package com.deadlock.hellocs.quiz.adapter.out.persistence.entity;

import com.deadlock.hellocs.global.entity.BaseJpaEntity;
import com.deadlock.hellocs.quiz.QuizLevel;
import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.domain.Quiz;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quizzes SET deleted_at = NOW() WHERE id = ?")
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
    @Builder.Default
    private List<Long> topicIds = new ArrayList<>();

    public abstract Quiz toDomain();
}
