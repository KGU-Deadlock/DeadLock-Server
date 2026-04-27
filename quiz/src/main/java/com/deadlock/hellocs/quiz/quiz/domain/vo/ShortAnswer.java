package com.deadlock.hellocs.quiz.quiz.domain.vo;

public record ShortAnswer(String value) implements QuizAnswer {
    @Override
    public String asString() {
        return value;
    }

    public static ShortAnswer of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("주관식 정답은 비어 있을 수 없습니다.");
        }
        return new ShortAnswer(value.trim());
    }
}
