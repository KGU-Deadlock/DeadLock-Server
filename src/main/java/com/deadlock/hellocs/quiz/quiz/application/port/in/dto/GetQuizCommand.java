package com.deadlock.hellocs.quiz.quiz.application.port.in.dto;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Quiz 조회 요청 DTO
 */
public record GetQuizCommand(
        @NotNull(message = "퀴즈 난이도는 필수입니다.")
        QuizLevel level,
        @NotEmpty(message = "최소 하나 이상의 토픽 ID가 필요합니다.")
        List<@NotNull(message = "토픽 ID는 null일 수 없습니다.") Long> topicIds,
        @NotNull(message = "퀴즈 모드는 필수입니다.")
        QuizMode mode
) {
}
