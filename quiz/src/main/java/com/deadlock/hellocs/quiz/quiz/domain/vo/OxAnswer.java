package com.deadlock.hellocs.quiz.quiz.domain.vo;

public record OxAnswer(Boolean value) implements QuizAnswer {
    @Override
    public String asString() {
        return String.valueOf(value);
    }

    public static OxAnswer of(Boolean value) {
        if (value == null) {
            throw new IllegalArgumentException("OX 정답은 null일 수 없습니다.");
        }
        return new OxAnswer(value);
    }

    public static OxAnswer ofTrue() {
        return new OxAnswer(true);
    }

    public static OxAnswer ofFalse() {
        return new OxAnswer(false);
    }
}
