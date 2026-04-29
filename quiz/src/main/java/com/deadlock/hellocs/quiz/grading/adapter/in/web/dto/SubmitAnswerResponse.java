package com.deadlock.hellocs.quiz.grading.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record SubmitAnswerResponse(
        @Schema(description = "생성된 채점 로그 ID", example = "log-12345")
        String gradingLogId
) {
}
