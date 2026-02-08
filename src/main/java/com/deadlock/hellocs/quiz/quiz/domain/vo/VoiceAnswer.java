package com.deadlock.hellocs.quiz.quiz.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class VoiceAnswer implements QuizAnswer {
    private final String value;
    
    @Override
    public String asString() {
        return value;
    }

    public static VoiceAnswer of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Voice answer cannot be empty");
        }
        return new VoiceAnswer(value.trim());
    }
}
