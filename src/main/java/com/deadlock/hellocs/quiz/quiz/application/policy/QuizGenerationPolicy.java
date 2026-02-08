package com.deadlock.hellocs.quiz.quiz.application.policy;

import com.deadlock.hellocs.quiz.shared.domain.QuizMode;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;

import java.util.Map;

/**
 * Quiz 생성 정책
 * 
 * 모드별로 어떤 타입의 Quiz를 몇 개씩 생성할지 정의
 */
public interface QuizGenerationPolicy {
    /**
     * 특정 모드를 지원하는지 확인
     * 
     * @param mode Quiz 모드
     * @return 지원 여부
     */
    boolean supports(QuizMode mode);
    
    /**
     * Quiz 타입별 생성 개수 반환
     * 
     * @return Map<QuizType, 개수>
     */
    Map<QuizType, Integer> getQuizComposition();
}
