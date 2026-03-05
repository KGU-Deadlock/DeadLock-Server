package com.deadlock.hellocs.user.application.port.in;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.user.application.port.in.dto.ProfileResult;



public interface LoadUserUseCase {
    ProfileResult getProfile(Long kakaoId);
    QuizLevel getUserLevel(Long kakaoId);
    boolean isExist(Long kakaoId);
}
