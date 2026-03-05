package com.deadlock.hellocs.user.application.service;

import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.user.application.port.in.ManageUserUseCase;
import com.deadlock.hellocs.user.application.port.in.CreateUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserSummaryUseCase;
import com.deadlock.hellocs.user.application.port.in.UserSummaryResult;
import com.deadlock.hellocs.user.application.port.out.LoadTopicPort;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.application.port.out.SaveUserPort;
import com.deadlock.hellocs.user.adapter.in.web.dto.MyInfoResponse;
import com.deadlock.hellocs.user.domain.User;
import com.deadlock.hellocs.user.adapter.in.web.dto.ProfileResponse;
import com.deadlock.hellocs.user.adapter.in.web.dto.UpdateMyInfoRequest;
import com.deadlock.hellocs.user.adapter.in.web.dto.UserSignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements CreateUserUseCase, LoadUserUseCase, LoadUserSummaryUseCase, ManageUserUseCase {

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
    @Transactional(readOnly = true)
    public boolean isExist(Long kakaoId) {
        try {
            loadUserPort.loadUserByKakaoId(kakaoId);
            return true;
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorStatus._USER_NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResult getUserSummary(Long kakaoId) {
        User user = loadUserPort.loadUserByKakaoId(kakaoId);
        return new UserSummaryResult(user.getKakaoId(), user.getNickname(), user.getProfileImage());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResult> getUserSummaries(List<Long> kakaoIds) {
        return loadUserPort.loadUsersByKakaoIds(kakaoIds).stream()
                .map(user -> new UserSummaryResult(user.getKakaoId(), user.getNickname(), user.getProfileImage()))
                .toList();
    }

    @Override
    public MyInfoResponse updateMyInfo(Long kakaoId, UpdateMyInfoRequest request) {
        User user = loadUserPort.loadUserByKakaoId(kakaoId);

        if (request.nickname() != null
                && !request.nickname().equals(user.getNickname())
                && loadUserPort.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorStatus._NICKNAME_ALREADY_EXISTS);
        }

        String changedNickname = request.nickname() != null ? request.nickname() : user.getNickname();
        String changedProfileImage = request.profileImage() != null ? request.profileImage() : user.getProfileImage();

        user.updateProfile(changedNickname, changedProfileImage, user.getInterestTopicIds());
        saveUserPort.saveUser(user);

        return MyInfoResponse.from(user);
    }

    @Override
    public void deleteMyAccount(Long kakaoId) {
        saveUserPort.deleteUserByKakaoId(kakaoId);
    }
}
