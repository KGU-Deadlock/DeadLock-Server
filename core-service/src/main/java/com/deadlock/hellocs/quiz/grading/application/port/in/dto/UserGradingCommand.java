package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 사용자 답안 DTO
 */
public record UserGradingCommand(
        @Schema(description = "답안을 제출할 퀴즈 ID", example = "101")
        @NotNull(message = "퀴즈 ID는 필수입니다.")
        @Positive(message = "퀴즈 ID는 1 이상이어야 합니다.")
        Long quizId,
        @Schema(description = "사용자 답안", example = "true")
        @NotBlank(message = "답안은 비어 있을 수 없습니다.")
        String answer
) {}
