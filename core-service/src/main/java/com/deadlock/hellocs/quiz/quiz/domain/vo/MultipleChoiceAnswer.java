package com.deadlock.hellocs.quiz.quiz.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class MultipleChoiceAnswer implements QuizAnswer {
    private final Integer value;

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    public static MultipleChoiceAnswer of(Integer value) {
        if (value == null || value < 1 || value > 5) {
            throw new IllegalArgumentException("객관식 정답은 1 이상 5 이하여야 합니다.");
        }
        return new MultipleChoiceAnswer(value);
    }
}
