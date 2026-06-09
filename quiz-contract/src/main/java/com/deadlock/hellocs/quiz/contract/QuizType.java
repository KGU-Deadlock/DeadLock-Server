package com.deadlock.hellocs.quiz.contract;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuizType {
    OX("OX"),
    MULTIPLE_CHOICE("객관식"),
    SHORT_ANSWER("주관식"),
    VOICE("음성");

    private final String description;
}
