package com.deadlock.hellocs.quiz.quiz.domain.vo;

public record VoiceAnswer(String value) implements QuizAnswer {
    @Override
    public String asString() {
        return value;
    }

    public static VoiceAnswer of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("음성형 정답은 비어 있을 수 없습니다.");
        }
        return new VoiceAnswer(value.trim());
    }
}
