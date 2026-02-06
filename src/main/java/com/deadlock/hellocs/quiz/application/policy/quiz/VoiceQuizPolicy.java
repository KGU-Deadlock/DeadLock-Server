package com.deadlock.hellocs.quiz.application.policy.quiz;

import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.application.port.in.QuizMode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VoiceQuizPolicy implements QuizGenerationPolicy {

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
