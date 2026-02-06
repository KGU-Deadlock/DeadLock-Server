package com.deadlock.hellocs.quiz.adapter.out.external;

import com.deadlock.hellocs.quiz.application.port.in.response.GradingResult;
import com.deadlock.hellocs.quiz.application.port.out.AiGradingPort;
import com.deadlock.hellocs.quiz.domain.Quiz;
import org.springframework.stereotype.Component;

@Component
public class AiGradingAdapter implements AiGradingPort {

    @Override
    public GradingResult gradeWithAi(Quiz quiz, String userAnswer) {
        // TODO: Implement actual AI service call (e.g., OpenAI API)
        
        // Mock implementation for now
        boolean isCorrect = userAnswer != null && userAnswer.length() > 5; // Simple dummy logic
        int score = isCorrect ? 85 : 40;
        String feedback = isCorrect 
                ? "좋은 답변입니다. 핵심 키워드가 잘 포함되어 있습니다." 
                : "답변이 다소 부족합니다. 조금 더 구체적으로 설명해주세요.";

        return GradingResult.builder()
                .quizId(quiz.getId())
                .correctAnswer(quiz.getCorrectAnswerStr())
                .feedback(feedback)
                .score(score)
                .build();
    }
}
