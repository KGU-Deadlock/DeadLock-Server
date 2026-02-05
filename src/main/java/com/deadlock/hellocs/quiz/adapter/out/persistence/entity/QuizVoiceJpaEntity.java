package com.deadlock.hellocs.quiz.adapter.out.persistence.entity;

import com.deadlock.hellocs.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.domain.QuizVoice;
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
@Table(name = "quiz_voice")
@DiscriminatorValue("VOICE")
public class QuizVoiceJpaEntity extends QuizJpaEntity {

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "explain", columnDefinition = "TEXT")
    private String explain;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Override
    public Quiz toDomain() {
        return QuizVoice.builder()
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
                .contentText(this.contentText)
                .build();
    }

    public static QuizVoiceJpaEntity from(QuizVoice quiz) {
        return QuizVoiceJpaEntity.builder()
                .id(quiz.getId())
                .level(quiz.getLevel())
                .content(quiz.getContent())
                .answer(quiz.getAnswer())
                .explain(quiz.getExplain())
                .contentText(quiz.getContentText())
                .topicIds(quiz.getTopicIds())
                .build();
    }
}
