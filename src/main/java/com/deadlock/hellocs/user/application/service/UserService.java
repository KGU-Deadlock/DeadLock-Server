package com.deadlock.hellocs.user.application.service;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.user.application.port.in.CreateUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.application.port.out.LoadTopicPort;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.application.port.out.SaveUserPort;
import com.deadlock.hellocs.user.domain.User;
import com.deadlock.hellocs.user.adapter.in.web.dto.ProfileResponse;
import com.deadlock.hellocs.user.adapter.in.web.dto.UserSignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements CreateUserUseCase, LoadUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadTopicPort loadTopicPort;

    @Override
    public void createUser(Long kakaoId, UserSignUpRequest userInfo) {
        // 관심사 이름으로 Topic ID 조회
        List<Long> interestTopicIds = loadTopicPort.getTopicIdsByNames(userInfo.interests());

        User user = User.createUser(
                kakaoId,
                userInfo.kakaoEmail(),
                userInfo.nickname(),
                userInfo.quizLevel()
        );
        
        // 관심사 설정
        user.updateProfile(user.getNickname(), user.getProfileImage(), interestTopicIds);

        saveUserPort.saveUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long kakaoId) {
        User user = loadUserPort.loadUserByKakaoId(kakaoId);

        List<Long> interestIds = user.getInterestTopicIds();
        List<String> interests = loadTopicPort.getTopicNamesByIds(interestIds);

        return new ProfileResponse(user.getProfileImage(), user.getNickname(), interests);
    }

    @Override
    public QuizLevel getUserLevel(Long kakaoId) {
        User user = loadUserPort.loadUserByKakaoId(kakaoId);
        return user.getQuizLevel();
    }

    @Override
    public boolean isExist(Long kakaoId) {
        return getProfile(kakaoId) != null;
    }
}
