package com.deadlock.hellocs.quiz.quiz.domain;

import com.deadlock.hellocs.quiz.quiz.domain.vo.MultipleChoiceAnswer;
import com.deadlock.hellocs.quiz.quiz.domain.vo.QuizAnswer;
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
    private Integer choice;
    
    @Override
    public QuizAnswer getAnswer() {
        return MultipleChoiceAnswer.of(answer);
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
