package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 채점 로그 조회 요청 DTO
 */
public record GetGradingLogCommand(
        @NotBlank(message = "채점 로그 ID는 필수입니다.")
        String gradingLogId
) {
}
