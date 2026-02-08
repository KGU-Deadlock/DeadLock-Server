package com.deadlock.hellocs.quiz.grading.application.policy;

import com.deadlock.hellocs.quiz.grading.application.port.out.AiGradingOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingResult;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 채점 정책
 * 
 * 단답형, 음성 Quiz에 사용
 * AI를 통한 유연한 채점 (부분 점수 가능)
 */
@Component
@RequiredArgsConstructor
public class AiGradingPolicy implements GradingPolicy {
    
    private final AiGradingOutputPort aiGradingPort;
    
    @Override
    public boolean supports(QuizType type) {
        return type == QuizType.SHORT_ANSWER || type == QuizType.VOICE;
    }
    
    @Override
    public GradingResult grade(Quiz quiz, String userAnswer) {
        return aiGradingPort.gradeWithAi(quiz, userAnswer);
    }
}
