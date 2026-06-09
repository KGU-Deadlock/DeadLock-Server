package com.deadlock.hellocs.quiz.application.port.in.dto;

import com.deadlock.hellocs.quiz.domain.QuizVoice;

public record VoiceQuizResult(
        Long id,
        String content,
        String contentText
) {
    public static VoiceQuizResult from(QuizVoice quiz) {
        return new VoiceQuizResult(
                quiz.getId(),
                quiz.getContent(),
                quiz.getContentText()
        );
    }
}
