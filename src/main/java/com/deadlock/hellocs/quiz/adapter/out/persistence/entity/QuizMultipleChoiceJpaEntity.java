package com.deadlock.hellocs.quiz.adapter.out.persistence.entity;

import com.deadlock.hellocs.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.domain.QuizMultipleChoice;
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
@Table(name = "quiz_multiple_choice")
@DiscriminatorValue("MULTIPLE_CHOICE")
public class QuizMultipleChoiceJpaEntity extends QuizJpaEntity {

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "answer", nullable = false)
    private Integer answer;

    @Column(name = "explain", columnDefinition = "TEXT")
    private String explain;

    @Column(name = "choice", columnDefinition = "TEXT")
    private String choice;

    @Override
    public Quiz toDomain() {
        return QuizMultipleChoice.builder()
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
                .choice(this.choice)
                .build();
    }

    public static QuizMultipleChoiceJpaEntity from(QuizMultipleChoice quiz) {
        return QuizMultipleChoiceJpaEntity.builder()
                .id(quiz.getId())
                .level(quiz.getLevel())
                .content(quiz.getContent())
                .answer(quiz.getAnswer())
                .explain(quiz.getExplain())
                .choice(quiz.getChoice())
                .topicIds(quiz.getTopicIds())
                .build();
    }
}
