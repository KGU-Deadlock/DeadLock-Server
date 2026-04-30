package com.deadlock.hellocs.interview.feedback.application.port.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiFeedbackResponse(
        @JsonProperty("score") int score,
        @JsonProperty("missing_keywords") List<String> missingKeywords,
        @JsonProperty("improved_answer") String improvedAnswer,
        @JsonProperty("message") String message
) {}
