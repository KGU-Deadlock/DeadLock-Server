package com.deadlock.hellocs.interview.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InterviewFeedback {
    private Long id;
    private String interviewId;
    private int overallScore;
    private List<QuestionFeedback> questionFeedbacks;
    private LocalDateTime createdAt;
}
