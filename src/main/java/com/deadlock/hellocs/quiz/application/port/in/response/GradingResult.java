package com.deadlock.hellocs.quiz.application.port.in.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GradingResult {
    private final Long quizId;
    private final String correctAnswer; // 정답 또는 모범 답안
    private final String feedback;      // 해설 또는 AI 피드백
    private final Integer score;        // 점수 (0~100)
}
