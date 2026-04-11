package com.deadlock.hellocs.streak.application.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StreakSummaryResult(
        // 수정 표시
        @Schema(description = "현재 연속 학습 일수입니다.", example = "4")
        int currentStreakDays,
        // 수정 표시
        @Schema(description = "누적 퀴즈 풀이 수입니다.", example = "87")
        int solvedQuizCount,
        // 수정 표시
        @Schema(description = "해결한 분야 수입니다.", example = "5")
        int solvedTopicCount
) {
}
