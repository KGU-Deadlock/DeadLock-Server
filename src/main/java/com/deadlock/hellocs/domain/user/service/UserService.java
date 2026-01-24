package com.deadlock.hellocs.domain.user.service;

import com.deadlock.hellocs.domain.user.dto.UserSignUpRequest;
import com.deadlock.hellocs.domain.user.entity.User;
import com.deadlock.hellocs.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public void createUser(Long kakaoId, UserSignUpRequest userInfo) {
        User user = User.builder()
                .kakaoId(kakaoId)
                .kakaoEmail(userInfo.kakaoEmail())
                .nickname(userInfo.nickname())
                .quizLevel(userInfo.quizLevel())
                .build();
        userRepository.save(user);
    }
}
