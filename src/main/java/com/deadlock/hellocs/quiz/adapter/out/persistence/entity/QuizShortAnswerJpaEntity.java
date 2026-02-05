package com.deadlock.hellocs.quiz.adapter.out.persistence.entity;

import com.deadlock.hellocs.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.domain.QuizShortAnswer;
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
@Table(name = "quiz_short_answer")
@DiscriminatorValue("SHORT_ANSWER")
public class QuizShortAnswerJpaEntity extends QuizJpaEntity {

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "explain", columnDefinition = "TEXT")
    private String explain;

    @Override
    public Quiz toDomain() {
        return QuizShortAnswer.builder()
                .id(getId())
                .level(getLevel())
                .type(getType())
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .topicIds(getTopicIds())
                .content(this.content)
                .answer(this.answer)
                .explain(this.explain)
                .build();
    }

    public static QuizShortAnswerJpaEntity from(QuizShortAnswer quiz) {
        return QuizShortAnswerJpaEntity.builder()
                .id(quiz.getId())
                .level(quiz.getLevel())
                .content(quiz.getContent())
                .answer(quiz.getAnswer())
                .explain(quiz.getExplain())
                .topicIds(quiz.getTopicIds())
                .build();
    }
}
