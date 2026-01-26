package com.deadlock.hellocs.domain.user.service;

import com.deadlock.hellocs.domain.user.dto.ProfileResponse;
import com.deadlock.hellocs.domain.user.entity.User;
import com.deadlock.hellocs.domain.user.entity.UserInterest;
import com.deadlock.hellocs.domain.user.port.out.TopicPort;
import com.deadlock.hellocs.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final TopicPort topicPort;

    /**
     * Builds a user's profile containing profile image, nickname, and interest names for the given Kakao ID.
     *
     * @param kakaoId the Kakao user identifier to look up
     * @return a ProfileResponse containing the user's profile image, nickname, and list of interest names
     * @throws RuntimeException if no user exists for the given kakaoId
     */
    public ProfileResponse getProfile(Long kakaoId) {
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<Long> interestIds = user.getInterests().stream().map(UserInterest::getTopicId).toList();
        List<String> interests = topicPort.getTopicNamesByIds(interestIds);

        return new ProfileResponse(user.getProfileImage(), user.getNickname(), interests);
    }
}