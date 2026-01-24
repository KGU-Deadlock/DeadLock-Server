package com.deadlock.hellocs.domain.user.controller;

import com.deadlock.hellocs.domain.user.dto.UserSignUpRequest;
import com.deadlock.hellocs.domain.user.service.UserService;
import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ApiResponse<Void> createUser(@RequestBody @Valid UserSignUpRequest userInfo,
                                                        @AuthenticationPrincipal Jwt jwt) {
        Long kakaoId = Long.valueOf(jwt.getSubject());

        userService.createUser(kakaoId, userInfo);
        return ApiResponse.onSuccess(null);
    }
}
