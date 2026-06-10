package com.deadlock.hellocs.interview.feedback.application.port.in.dto;

import com.deadlock.hellocs.interview.feedback.domain.QuestionFeedback;

import java.util.List;

public record GenerateFeedbackCommand(
        String interviewId,
        String companyName,
        String position,
        List<QuestionFeedback> questionFeedbacks
) {}
