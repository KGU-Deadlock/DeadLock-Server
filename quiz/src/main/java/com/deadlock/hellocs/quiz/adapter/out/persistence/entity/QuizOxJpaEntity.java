package com.deadlock.hellocs.quiz.adapter.out.persistence.entity;

import com.deadlock.hellocs.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.domain.QuizOx;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_ox")
@DiscriminatorValue("OX")
public class QuizOxJpaEntity extends QuizJpaEntity {
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "answer", nullable = false)
    private Boolean answer;
    
    @Column(name = "explain", columnDefinition = "TEXT")
    private String explain;
    
    @Override
    public String correctAnswerAsString() {
        return String.valueOf(answer);
    }

    @Override
    public Quiz toDomain() {
        return QuizOx.builder()
                .id(getId())
                .level(getLevel())
                .type(getType())
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .topicIds(getTopicIds())
                .content(content)
                .answer(answer)
                .explain(explain)
                .build();
    }
}
