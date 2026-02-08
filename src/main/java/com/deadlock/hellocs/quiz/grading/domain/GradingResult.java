package com.deadlock.hellocs.quiz.grading.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * 채점 결과 도메인 객체
 */
@Getter
@Builder
public class GradingResult {
    private final Long quizId;
    private final String answer;
    private final String feedback;
    private final Integer score;  // 0~100
}
