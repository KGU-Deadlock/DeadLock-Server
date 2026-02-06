package com.deadlock.hellocs.quiz.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizVoice extends Quiz {
    private String content;
    private String answer;
    private String explain;
    private String contentText;

    @Override
    public boolean isMatch(String answer) {
        return false;
    }

    @Override
    public String getCorrectAnswerStr() {
        return this.answer;
    }
}
