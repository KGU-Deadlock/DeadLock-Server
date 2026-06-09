package com.deadlock.hellocs.quiz.application.port.out;

import com.deadlock.hellocs.quiz.contract.QuizLevel;

public interface QueryUserOutputPort {
    QuizLevel getUserLevel(Long kakaoId);
}
