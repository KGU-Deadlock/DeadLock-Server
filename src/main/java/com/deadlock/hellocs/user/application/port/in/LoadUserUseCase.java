package com.deadlock.hellocs.user.application.port.in;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.user.adapter.in.web.dto.ProfileResponse;
import org.springframework.modulith.NamedInterface;

@NamedInterface
public interface LoadUserUseCase {
    ProfileResponse getProfile(Long kakaoId);
    QuizLevel getUserLevel(Long kakaoId);
    boolean isExist(Long kakaoId);
}
