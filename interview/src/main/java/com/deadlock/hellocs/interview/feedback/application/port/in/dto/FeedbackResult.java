package com.deadlock.hellocs.interview.feedback.application.port.in.dto;

import com.deadlock.hellocs.interview.feedback.domain.QuestionFeedback;

import java.util.List;

public record FeedbackResult(
        int overallScore,
        List<QuestionFeedback> questionFeedbacks
) {}
