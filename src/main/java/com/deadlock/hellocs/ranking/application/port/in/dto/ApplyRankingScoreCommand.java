package com.deadlock.hellocs.ranking.application.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record ApplyRankingScoreCommand(
        @NotBlank String gradingLogId,
        @NotNull Long kakaoId,
        // 수정 표시
        @PositiveOrZero int totalScore,
        // 수정 표시
        @NotNull List<Long> topicIds
) {
}
