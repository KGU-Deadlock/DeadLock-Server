package com.deadlock.hellocs.interview.feedback.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionFeedback {
    private int questionNumber;
    private int score;
    private List<String> missingKeywords;
    private String improvedAnswer;
    private String message;
}
