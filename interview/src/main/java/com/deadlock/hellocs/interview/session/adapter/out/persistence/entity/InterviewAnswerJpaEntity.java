package com.deadlock.hellocs.interview.session.adapter.out.persistence.entity;

import com.deadlock.hellocs.interview.feedback.domain.QuestionFeedback;
import com.deadlock.hellocs.interview.session.domain.InterviewAnswer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    public InterviewAnswer toDomain() {
        return InterviewAnswer.builder()
                .id(id)
                .interviewId(interviewId)
                .questionNumber(questionNumber)
                .answerText(answerText)
                .durationSeconds(durationSeconds)
                .answeredAt(answeredAt)
                .questionFeedback(parseAiFeedback())
                .build();
    }

    public static InterviewAnswerJpaEntity from(InterviewAnswer answer) {
        return InterviewAnswerJpaEntity.builder()
                .interviewId(answer.getInterviewId())
                .questionNumber(answer.getQuestionNumber())
                .answerText(answer.getAnswerText())
                .durationSeconds(answer.getDurationSeconds())
                .answeredAt(answer.getAnsweredAt())
                .aiFeedback(serializeQuestionFeedback(answer.getQuestionFeedback()))
                .build();
    }

    private QuestionFeedback parseAiFeedback() {
        if (aiFeedback == null || aiFeedback.isBlank()) return null;
        try {
            return MAPPER.readValue(aiFeedback, QuestionFeedback.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String serializeQuestionFeedback(QuestionFeedback feedback) {
        if (feedback == null) return null;
        try {
            return MAPPER.writeValueAsString(feedback);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
