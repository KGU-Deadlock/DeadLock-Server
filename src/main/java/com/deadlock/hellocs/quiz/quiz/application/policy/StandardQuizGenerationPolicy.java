package com.deadlock.hellocs.quiz.quiz.application.policy;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 표준 Quiz 생성 정책
 * 
 * OX 2개 + 객관식 2개 + 단답형 1개 = 총 5개
 */
@Component
public class StandardQuizGenerationPolicy implements QuizGenerationPolicy {
    
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
