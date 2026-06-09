package com.deadlock.hellocs.grading.application.port.in.dto;

import jakarta.validation.constraints.NotBlank;

public record GetGradingLogCommand(
        @NotBlank(message = "채점 로그 ID는 필수입니다.")
        String gradingLogId
) {
}
