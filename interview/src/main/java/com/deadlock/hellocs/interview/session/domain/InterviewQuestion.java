package com.deadlock.hellocs.interview.session.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterviewQuestion {
    private Long id;
    private String interviewId;
    private int questionNumber;
    private String questionText;
    private QuestionType questionType;
    private String category;
}
