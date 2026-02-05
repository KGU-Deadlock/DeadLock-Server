package com.deadlock.hellocs.user.adapter.in.web;

import com.deadlock.hellocs.user.application.port.in.CreateUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.adapter.in.web.dto.ProfileResponse;
import com.deadlock.hellocs.user.adapter.in.web.dto.UserSignUpRequest;
import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final LoadUserUseCase loadUserUseCase;

    @PostMapping
    public ApiResponse<Void> createUser(@RequestBody @Valid UserSignUpRequest userInfo,
                                        @AuthenticationPrincipal Jwt jwt) {
        Long kakaoId = Long.valueOf(jwt.getSubject());
        createUserUseCase.createUser(kakaoId, userInfo);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long kakaoId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(loadUserUseCase.getProfile(kakaoId));
    }
}
