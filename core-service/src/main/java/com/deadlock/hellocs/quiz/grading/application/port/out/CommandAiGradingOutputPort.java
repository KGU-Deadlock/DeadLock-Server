package com.deadlock.hellocs.quiz.grading.application.port.out;


import com.deadlock.hellocs.quiz.grading.domain.GradingItem;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;

/**
 * AI 채점 Output Port
 * 
 * External Adapter (AI Service)가 포트를 구현
 */
public interface CommandAiGradingOutputPort {
    /**
     * AI를 사용한 채점
     * 
     * @param quiz 문제
     * @param userAnswer 사용자 답안
     * @return 채점 결과
     */
    GradingItem gradeWithAi(Quiz quiz, String userAnswer);
}
