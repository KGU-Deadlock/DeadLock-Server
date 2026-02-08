package com.deadlock.hellocs.quiz.quiz.domain;

import com.deadlock.hellocs.quiz.quiz.domain.vo.QuizAnswer;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.quiz.shared.domain.QuizType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quiz 도메인 엔티티
 * 
 * 책임:
 * - 문제 정보 보관 (내용, 정답, 해설)
 * - 문제 메타데이터 관리 (레벨, 타입, 토픽)
 * 
 * 책임 아님:
 * - 채점 로직 (Grading Context가 담당)
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class Quiz {
    private Long id;
    private QuizLevel level;
    private QuizType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private List<Long> topicIds;
    
    /**
     * 문제의 정답을 반환
     * 채점은 하지 않고, 정답 정보만 제공
     */
    public abstract QuizAnswer getAnswer();
    
    /**
     * 문제 내용을 반환
     */
    public abstract String getContent();
    
    /**
     * 해설을 반환
     */
    public abstract String getExplain();
}
