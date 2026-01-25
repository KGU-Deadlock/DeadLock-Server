package com.deadlock.hellocs.domain.user.service;

import com.deadlock.hellocs.domain.interest.entity.Interest;
import com.deadlock.hellocs.domain.interest.repository.UserInterestRepository;
import com.deadlock.hellocs.domain.user.dto.MyProfileResponse;
import com.deadlock.hellocs.domain.user.entity.User;
import com.deadlock.hellocs.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileQueryService {

    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;

    public MyProfileResponse getMyProfile(Long kakaoId) {
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (Boolean.TRUE.equals(user.getDeleteAt())) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        List<Interest> interests = userInterestRepository.findInterestNamesByUserId(user.getId());
        return new MyProfileResponse(user.getProfileImage(), user.getNickname(), interests);
    }
}
