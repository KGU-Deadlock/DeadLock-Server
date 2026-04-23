package com.deadlock.hellocs.quiz.quiz.adapter.out.persistence.entity;

import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.quiz.domain.QuizVoice;
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
    
    @Column(name = "answer", nullable = false)
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
                .content(content)
                .answer(answer)
                .explain(explain)
                .contentText(contentText)
                .build();
    }
}
