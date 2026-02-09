package com.deadlock.hellocs.quiz.grading.application.port.in.dto;

/**
 * 사용자 답안 DTO
 */
public record UserGradingCommand(
        Long quizId,
        String answer
) {}
