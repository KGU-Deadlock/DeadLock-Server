package com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GetQuizRequest(
        @Schema(description = "토픽 ID 목록", example = "[1,2,3]")
        @NotEmpty(message = "최소 하나 이상의 토픽 ID가 필요합니다.")
        List<@NotNull(message = "토픽 ID는 null일 수 없습니다.") Long> topicIds,
        @Schema(description = "퀴즈 모드", example = "STANDARD")
        @NotNull(message = "퀴즈 모드는 필수입니다.")
        QuizMode mode
) {
}
