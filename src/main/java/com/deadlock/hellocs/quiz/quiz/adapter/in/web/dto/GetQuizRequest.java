package com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GetQuizRequest(
        @Schema(description = "토픽 ID 목록", example = "[1,2,3]")
        List<Long> topicIds,
        @Schema(description = "퀴즈 모드", example = "STANDARD")
        QuizMode mode
) {
}
