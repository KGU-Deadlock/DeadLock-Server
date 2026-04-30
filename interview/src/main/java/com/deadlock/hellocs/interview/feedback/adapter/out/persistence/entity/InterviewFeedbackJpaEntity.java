package com.deadlock.hellocs.interview.feedback.adapter.out.persistence.entity;

import com.deadlock.hellocs.interview.feedback.domain.InterviewFeedback;
import com.deadlock.hellocs.interview.feedback.domain.QuestionFeedback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "interview_feedbacks")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFeedbackJpaEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "interview_id", nullable = false, unique = true, length = 36)
    private String interviewId;

    @Column(name = "overall_score")
    private int overallScore;

    @Column(name = "detailed_feedback", columnDefinition = "TEXT")
    private String detailedFeedback;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public InterviewFeedback toDomain() {
        List<QuestionFeedback> questionFeedbacks = parseDetailedFeedback();
        return InterviewFeedback.builder()
                .id(id)
                .interviewId(interviewId)
                .overallScore(overallScore)
                .questionFeedbacks(questionFeedbacks)
                .createdAt(createdAt)
                .build();
    }

    public static InterviewFeedbackJpaEntity from(InterviewFeedback feedback) {
        return InterviewFeedbackJpaEntity.builder()
                .interviewId(feedback.getInterviewId())
                .overallScore(feedback.getOverallScore())
                .detailedFeedback(serializeFeedback(feedback))
                .build();
    }

    private List<QuestionFeedback> parseDetailedFeedback() {
        try {
            return MAPPER.readValue(detailedFeedback, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static String serializeFeedback(InterviewFeedback feedback) {
        try {
            return MAPPER.writeValueAsString(feedback.getQuestionFeedbacks());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
