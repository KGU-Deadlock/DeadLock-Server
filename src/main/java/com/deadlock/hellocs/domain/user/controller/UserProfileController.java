package com.deadlock.hellocs.domain.user.controller;

import com.deadlock.hellocs.domain.user.dto.MyProfileResponse;
import com.deadlock.hellocs.domain.user.service.UserProfileQueryService;
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

    private final UserProfileQueryService userProfileQueryService;

    @GetMapping("/me")
    public ApiResponse<MyProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long kakaoId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(userProfileQueryService.getMyProfile(kakaoId));
    }
}
