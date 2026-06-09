package com.deadlock.hellocs.grading.application.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GetGradingDetailLogCommand(
        @NotBlank(message = "채점 로그 ID는 필수입니다.")
        String gradingLogId,
        @NotNull(message = "퀴즈 ID는 필수입니다.")
        @Positive(message = "퀴즈 ID는 1 이상이어야 합니다.")
        Long quizId
) {
}
