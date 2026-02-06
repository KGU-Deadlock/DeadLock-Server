package com.deadlock.hellocs.quiz.application.policy.quiz;

import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.application.port.in.QuizMode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StandardQuizPolicy implements QuizGenerationPolicy {

    @Override
    public boolean supports(QuizMode mode) {
        return mode == QuizMode.STANDARD;
    }

    @Override
    public Map<QuizType, Integer> getQuizComposition() {
        return Map.of(
                QuizType.OX, 2,
                QuizType.MULTIPLE_CHOICE, 2,
                QuizType.SHORT_ANSWER, 1
        );
    }
}
