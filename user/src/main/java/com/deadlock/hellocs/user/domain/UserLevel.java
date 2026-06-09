package com.deadlock.hellocs.user.domain;

/**
 * 사용자 퀴즈 레벨을 나타내는 도메인 enum.
 * quiz 모듈의 QuizLevel과 동일한 값을 가지며, user 모듈이 quiz 모듈에 직접 의존하지 않도록 분리합니다.
 */
public enum UserLevel {
    JUNIOR,
    SEMIPRO,
    PRO
}
