package com.deadlock.hellocs.interview.question.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CsQuestion {
    private Long id;
    private QuestionCategory category;
    private String question;
    private Difficulty difficulty;
}
