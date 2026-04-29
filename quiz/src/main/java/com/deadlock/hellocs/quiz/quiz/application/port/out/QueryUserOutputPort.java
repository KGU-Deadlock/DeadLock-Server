package com.deadlock.hellocs.quiz.quiz.application.port.out;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;

public interface QueryUserOutputPort {
    QuizLevel getUserLevel(Long kakaoId);
}
