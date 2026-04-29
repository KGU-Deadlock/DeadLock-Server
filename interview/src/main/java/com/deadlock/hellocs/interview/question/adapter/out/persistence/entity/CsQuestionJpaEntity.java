package com.deadlock.hellocs.interview.question.adapter.out.persistence.entity;

import com.deadlock.hellocs.interview.question.domain.CsQuestion;
import com.deadlock.hellocs.interview.question.domain.Difficulty;
import com.deadlock.hellocs.interview.question.domain.QuestionCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cs_questions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsQuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public CsQuestion toDomain() {
        return CsQuestion.builder()
                .id(id)
                .category(category)
                .question(question)
                .difficulty(difficulty)
                .build();
    }
}
