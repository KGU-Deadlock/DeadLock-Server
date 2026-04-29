package com.deadlock.hellocs.interview.session.adapter.out.persistence.entity;

import com.deadlock.hellocs.interview.session.domain.InterviewQuestion;
import com.deadlock.hellocs.interview.session.domain.QuestionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interview_questions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interview_id", nullable = false, length = 36)
    private String interviewId;

    @Column(name = "question_number", nullable = false)
    private int questionNumber;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 50)
    private QuestionType questionType;

    @Column(length = 50)
    private String category;

    public InterviewQuestion toDomain() {
        return InterviewQuestion.builder()
                .id(id)
                .interviewId(interviewId)
                .questionNumber(questionNumber)
                .questionText(questionText)
                .questionType(questionType)
                .category(category)
                .build();
    }

    public static InterviewQuestionJpaEntity from(InterviewQuestion q) {
        return InterviewQuestionJpaEntity.builder()
                .interviewId(q.getInterviewId())
                .questionNumber(q.getQuestionNumber())
                .questionText(q.getQuestionText())
                .questionType(q.getQuestionType())
                .category(q.getCategory())
                .build();
    }
}
