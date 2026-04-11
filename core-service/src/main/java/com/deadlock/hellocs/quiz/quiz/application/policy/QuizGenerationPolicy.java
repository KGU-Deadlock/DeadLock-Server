package com.deadlock.hellocs.quiz.quiz.application.policy;

import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryQuizOutputPort;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import com.deadlock.hellocs.quiz.shared.domain.QuizMode;

import java.util.List;

/**
 * Quiz 생성 정책
 * 
 * 모드별로 퀴즈 생성 로직을 정의
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
     * 정책에 따라 Quiz 리스트 생성
     * 
     * @param command 퀴즈 조회 요청 커맨드
     * @param queryQuizPort 퀴즈 조회 포트
     * @return 생성된 Quiz 리스트
     */
    List<Quiz> generate(GetQuizCommand command, QueryQuizOutputPort queryQuizPort, Long userId);
}
