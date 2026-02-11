package com.deadlock.hellocs.quiz.grading.adapter.in.web.dto;

import lombok.Builder;

@Builder
public record SubmitAnswerResponse(
        String gradingLogId
) {
}
