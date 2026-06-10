package com.deadlock.hellocs.interview.session.domain;

import com.deadlock.hellocs.interview.feedback.domain.QuestionFeedback;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InterviewAnswer {
    private Long id;
    private String interviewId;
    private int questionNumber;
    private String answerText;
    private Integer durationSeconds;
    private LocalDateTime answeredAt;
    private QuestionFeedback questionFeedback;
}
