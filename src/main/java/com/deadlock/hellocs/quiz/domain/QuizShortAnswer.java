package com.deadlock.hellocs.quiz.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizShortAnswer extends Quiz {
    private String content;
    private String answer;
    private String explain;

    @Override
    public boolean isMatch(String answer) {
        return this.answer.equalsIgnoreCase(answer);
    }

    @Override
    public String getCorrectAnswerStr() {
        return this.answer;
    }
}
