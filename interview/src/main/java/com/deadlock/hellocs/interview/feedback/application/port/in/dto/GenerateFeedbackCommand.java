package com.deadlock.hellocs.interview.feedback.application.port.in.dto;

import java.util.List;

public record GenerateFeedbackCommand(
        String interviewId,
        String companyName,
        String position,
        List<QaPair> qaPairs
) {}
