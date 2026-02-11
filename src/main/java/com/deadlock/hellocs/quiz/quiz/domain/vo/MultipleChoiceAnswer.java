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
            throw new IllegalArgumentException("Multiple choice answer must be between 1 and 5");
        }
        return new MultipleChoiceAnswer(value);
    }
}
