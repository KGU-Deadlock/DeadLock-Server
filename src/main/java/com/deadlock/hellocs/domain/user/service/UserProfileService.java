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

    public ProfileResponse getProfile(Long kakaoId) {
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<Long> interestIds = user.getInterests().stream().map(UserInterest::getTopicId).toList();
        List<String> interests = topicPort.getTopicNamesByIds(interestIds);

        return new ProfileResponse(user.getProfileImage(), user.getNickname(), interests);
    }
}
