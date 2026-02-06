package com.deadlock.hellocs.quiz.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizMultipleChoice extends Quiz {
    private String content;
    private Integer answer;
    private String explain;
    private String choice;

    @Override
    public boolean isMatch(String answer) {
        return String.valueOf(this.answer).equals(answer);
    }

    @Override
    public String getCorrectAnswerStr() {
        return String.valueOf(this.answer);
    }
}
