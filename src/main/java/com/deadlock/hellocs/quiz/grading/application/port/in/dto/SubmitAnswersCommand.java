package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 답안 제출 요청 DTO
 */
public record SubmitAnswersCommand(
        @NotNull(message = "사용자 ID는 필수입니다.")
        @Positive(message = "사용자 ID는 1 이상이어야 합니다.")
        Long userId,
        @NotEmpty(message = "답안 목록은 비어 있을 수 없습니다.")
        List<@NotNull(message = "답안 항목은 null일 수 없습니다.") @Valid UserGradingCommand> answers
) {
}
