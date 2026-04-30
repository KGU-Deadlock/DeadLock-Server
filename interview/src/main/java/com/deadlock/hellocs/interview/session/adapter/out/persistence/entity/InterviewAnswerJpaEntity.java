package com.deadlock.hellocs.interview.session.adapter.out.persistence.entity;

import com.deadlock.hellocs.interview.session.domain.InterviewAnswer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_answers")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interview_id", nullable = false, length = 36)
    private String interviewId;

    @Column(name = "question_number", nullable = false)
    private int questionNumber;

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(name = "answered_at", updatable = false)
    private LocalDateTime answeredAt;

    public InterviewAnswer toDomain() {
        return InterviewAnswer.builder()
                .id(id)
                .interviewId(interviewId)
                .questionNumber(questionNumber)
                .answerText(answerText)
                .durationSeconds(durationSeconds)
                .answeredAt(answeredAt)
                .build();
    }

    public static InterviewAnswerJpaEntity from(InterviewAnswer answer) {
        return InterviewAnswerJpaEntity.builder()
                .interviewId(answer.getInterviewId())
                .questionNumber(answer.getQuestionNumber())
                .answerText(answer.getAnswerText())
                .durationSeconds(answer.getDurationSeconds())
                .answeredAt(answer.getAnsweredAt())
                .build();
    }
}
