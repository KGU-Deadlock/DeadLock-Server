package com.deadlock.hellocs.quiz.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizOx extends Quiz {
    private String content;
    private Boolean answer;
    private String explain;

    @Override
    public boolean isMatch(String answer) {
        return String.valueOf(this.answer).equalsIgnoreCase(answer);
    }

    @Override
    public String getCorrectAnswerStr() {
        return String.valueOf(this.answer);
    }
}
