package com.deadlock.hellocs.quiz.domain;

import com.deadlock.hellocs.quiz.QuizLevel;
import com.deadlock.hellocs.quiz.QuizType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class Quiz {
    private Long id;
    private QuizLevel level;
    private QuizType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private List<Long> topicIds;

    public abstract boolean isMatch(String answer);
    public abstract String getCorrectAnswerStr();
    public abstract String getExplain();
}
