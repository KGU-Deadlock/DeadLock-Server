package com.deadlock.hellocs.quiz.quiz.domain.vo;

/**
 * Quiz 정답을 표현하는 값 객체
 * 채점 로직은 포함하지 않고, 정답 정보만 보관
 */
// TODO: 필요성 검토. 단순히 String으로 바꾸기 위한 용도인데, 쓸모없으려나?
public interface QuizAnswer {
    /**
     * 정답을 문자열로 반환
     */
    String asString();
}
