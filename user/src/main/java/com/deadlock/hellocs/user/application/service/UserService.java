package com.deadlock.hellocs.user.application.service;

import com.deadlock.hellocs.user.domain.Role;
import com.deadlock.hellocs.user.domain.User;
import com.deadlock.hellocs.user.domain.UserLevel;
import com.deadlock.hellocs.common.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.user.application.port.in.ManageUserUseCase;
import com.deadlock.hellocs.user.application.port.in.CreateUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserSummaryUseCase;
import com.deadlock.hellocs.user.application.port.in.UserSummaryResult;
import com.deadlock.hellocs.user.application.port.in.dto.ProfileResult;
import com.deadlock.hellocs.user.application.port.in.dto.UpdateMyInfoCommand;
import com.deadlock.hellocs.user.application.port.in.dto.UserProfileSummaryResult;
import com.deadlock.hellocs.user.application.port.in.dto.UserSignUpCommand;
import com.deadlock.hellocs.user.application.port.out.LoadTopicPort;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.application.port.out.SaveUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements CreateUserUseCase, LoadUserUseCase, LoadUserSummaryUseCase, ManageUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadTopicPort loadTopicPort;

    @Override
    public void createUser(Long kakaoId, UserSignUpCommand command) {
        if (loadUserPort.existsByKakaoId(kakaoId)) {
            throw new CustomException(ErrorStatus._USER_ALREADY_EXISTS);
        }

        User user = User.createUser(
                kakaoId,
                command.kakaoEmail(),
                command.nickname(),
                command.profileImage(),
                command.quizLevel(),
                command.interests()
        );

        saveUserPort.saveUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResult getProfile(Long kakaoId) {
        User user = loadUserPort.loadUserByKakaoId(kakaoId);

        List<Long> interestIds = user.getInterestTopicIds();
        List<String> interests = loadTopicPort.getTopicNamesByIds(interestIds);

        return new ProfileResult(user.getProfileImage(), user.getNickname(), interests);
    }

    @Override
    public UserLevel getUserLevel(Long kakaoId) {
        User user = loadUserPort.loadUserByKakaoId(kakaoId);
        return user.getQuizLevel();
    }

    @Override
    @Transactional(readOnly = true)
    public Role getUserRole(Long kakaoId) {
        User user = loadUserPort.loadUserByKakaoId(kakaoId);
        return user.getRole();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isExist(Long kakaoId) {
        return loadUserPort.existsByKakaoId(kakaoId);
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
    public void updateMyInfo(Long kakaoId, UpdateMyInfoCommand command) {
        validateBlankField(command);

        User user = loadUserPort.loadUserByKakaoId(kakaoId);

        if (command.nickname() != null
                && !command.nickname().equals(user.getNickname())
                && loadUserPort.existsByNickname(command.nickname())) {
            throw new CustomException(ErrorStatus._NICKNAME_ALREADY_EXISTS);
        }

        user.update(command);
        saveUserPort.saveUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getInterestTopicIds(Long kakaoId) {
        return loadUserPort.loadUserByKakaoId(kakaoId).getInterestTopicIds();
    }

    @Override
    public void deleteMyAccount(Long kakaoId) {
        saveUserPort.deleteUserByKakaoId(kakaoId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileSummaryResult getProfileSummary(Long kakaoId) {
        User user = loadUserPort.loadUserByKakaoId(kakaoId);
        List<String> interests = loadTopicPort.getTopicNamesByIds(user.getInterestTopicIds());
        return new UserProfileSummaryResult(kakaoId, user.getNickname(), user.getProfileImage(), interests);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileSummaryResult> getProfileSummaries(List<Long> kakaoIds) {
        if (kakaoIds.isEmpty()) return List.of();

        List<User> users = loadUserPort.loadUsersByKakaoIds(kakaoIds);

        List<Long> allTopicIds = users.stream()
                .flatMap(u -> u.getInterestTopicIds().stream())
                .distinct()
                .toList();

        Map<Long, String> topicNameMap = allTopicIds.isEmpty()
                ? Map.of()
                : loadTopicPort.getTopicNameMapByIds(allTopicIds);

        return users.stream()
                .map(user -> new UserProfileSummaryResult(
                        user.getKakaoId(),
                        user.getNickname(),
                        user.getProfileImage(),
                        user.getInterestTopicIds().stream()
                                .map(topicNameMap::get)
                                .filter(Objects::nonNull)
                                .toList()
                ))
                .toList();
    }

    private void validateBlankField(UpdateMyInfoCommand command) {
        if (isBlank(command.nickname()) || isBlank(command.profileImage())) {
            throw new CustomException(ErrorStatus._BAD_REQUEST);
        }
    }

    private boolean isBlank(String value) {
        return value != null && value.isBlank();
    }
}
