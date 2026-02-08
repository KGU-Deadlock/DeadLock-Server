package com.deadlock.hellocs.quiz.quiz.application.policy;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 음성 Quiz 생성 정책
 * 
 * Voice 3개
 */
@Component
public class VoiceQuizGenerationPolicy implements QuizGenerationPolicy {
    
    @Override
    public boolean supports(QuizMode mode) {
        return mode == QuizMode.VOICE;
    }
    
    @Override
    public Map<QuizType, Integer> getQuizComposition() {
        return Map.of(
                QuizType.VOICE, 3
        );
    }
}
