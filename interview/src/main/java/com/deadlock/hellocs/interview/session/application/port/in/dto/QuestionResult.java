package com.deadlock.hellocs.interview.session.application.port.in.dto;

public record QuestionResult(
        int questionNumber,
        String questionText,
        String questionType,
        String category
) {}
