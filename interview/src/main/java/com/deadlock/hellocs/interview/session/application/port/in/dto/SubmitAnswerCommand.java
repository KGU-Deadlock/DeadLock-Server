package com.deadlock.hellocs.interview.session.application.port.in.dto;

public record SubmitAnswerCommand(
        String interviewId,
        int questionNumber,
        String answerText,
        Integer durationSeconds
) {}
