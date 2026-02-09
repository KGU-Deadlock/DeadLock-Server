package com.deadlock.hellocs.quiz.grading.adapter.out.external;

import com.deadlock.hellocs.quiz.grading.application.port.out.CommandAiGradingOutputPort;
import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import org.springframework.stereotype.Component;

/**
 * AI 채점 External Adapter
 * 
 * 실제 AI 서비스(OpenAI, Claude 등) 호출
 * 현재는 Mock 구현
 */
@Component
public class AiGradingAdapter implements CommandAiGradingOutputPort {
    
    @Override
    public GradingItem gradeWithAi(Quiz quiz, String userAnswer) {
        // TODO: 실제 AI 서비스 호출 구현
        // 예: OpenAI API, Claude API 등
        
        // Mock 구현
        boolean isReasonable = userAnswer != null && userAnswer.length() > 5;
        int score = calculateMockScore(userAnswer, quiz.getAnswer().asString());
        String feedback = generateMockFeedback(score);
        
        return GradingItem.builder()
                .quizId(quiz.getId())
                .score(score)
                .isCorrect(score >= 70)
                .userAnswer(userAnswer)
                .feedback(feedback)
                .build();
    }
    
    /**
     * Mock 점수 계산
     */
    private int calculateMockScore(String userAnswer, String answer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return 0;
        }
        
        // 간단한 유사도 체크
        String normalizedUser = userAnswer.toLowerCase().trim();
        String normalizedCorrect = answer.toLowerCase().trim();
        
        if (normalizedUser.equals(normalizedCorrect)) {
            return 100;
        }
        
        // 부분 점수 로직
        if (normalizedUser.contains(normalizedCorrect) || 
            normalizedCorrect.contains(normalizedUser)) {
            return 70;
        }
        
        // 단어 길이 유사하면 부분 점수
        if (normalizedUser.length() > 5) {
            return 40;
        }
        
        return 20;
    }
    
    /**
     * Mock 피드백 생성
     */
    private String generateMockFeedback(int score) {
        if (score >= 100) {
            return "완벽합니다! 정확한 답변입니다.";
        } else if (score >= 70) {
            return "좋습니다! 핵심 내용을 포함하고 있습니다.";
        } else if (score >= 40) {
            return "답변이 다소 부족합니다. 좀 더 구체적으로 써주세요.";
        } else {
            return "정답과 거리가 멉니다. 다시 한번 생각해보세요.";
        }
    }
}
