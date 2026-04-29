package com.deadlock.hellocs.interview.feedback.application.port.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiFeedbackRequest(
        @JsonProperty("question") String question,
        @JsonProperty("user_answer") String userAnswer,
        @JsonProperty("model_answer") String modelAnswer
) {}
