package com.deadlock.hellocs.quiz.quiz.domain;

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
    public String getAnswerAsString() {
        return String.valueOf(answer);
    }
    
    @Override
    public String getContent() {
        return content;
    }
    
    @Override
    public String getExplain() {
        return explain;
    }
}
