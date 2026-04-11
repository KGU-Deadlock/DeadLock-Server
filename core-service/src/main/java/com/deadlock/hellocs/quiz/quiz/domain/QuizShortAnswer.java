package com.deadlock.hellocs.quiz.quiz.domain;

import com.deadlock.hellocs.quiz.quiz.domain.vo.QuizAnswer;
import com.deadlock.hellocs.quiz.quiz.domain.vo.ShortAnswer;
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
    public QuizAnswer getAnswer() {
        return ShortAnswer.of(answer);
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
