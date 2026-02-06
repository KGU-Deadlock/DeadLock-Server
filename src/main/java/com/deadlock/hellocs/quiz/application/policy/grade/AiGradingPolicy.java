package com.deadlock.hellocs.quiz.application.policy.grade;

import com.deadlock.hellocs.quiz.QuizType;
import com.deadlock.hellocs.quiz.application.port.in.response.GradingResult;
import com.deadlock.hellocs.quiz.application.port.out.AiGradingPort;
import com.deadlock.hellocs.quiz.domain.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiGradingPolicy implements GradingPolicy {

    private final AiGradingPort aiGradingPort;

    @Override
    public boolean supports(QuizType type) {
        return type == QuizType.SHORT_ANSWER || type == QuizType.VOICE;
    }

    @Override
    public GradingResult grade(Quiz quiz, String userAnswer) {
        return aiGradingPort.gradeWithAi(quiz, userAnswer);
    }
}
