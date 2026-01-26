package com.deadlock.hellocs.domain.user.controller;

import com.deadlock.hellocs.domain.user.dto.ProfileResponse;
import com.deadlock.hellocs.domain.user.service.UserProfileService;
import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long kakaoId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(userProfileService.getProfile(kakaoId));
    }
}
