package com.deadlock.hellocs.global.auth.service;

import com.deadlock.hellocs.global.auth.client.KakaoAuthApiClient;
import com.deadlock.hellocs.global.auth.controller.AuthController;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoCodeAuthService {

    private final KakaoAuthApiClient kakaoAuthApiClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoadUserUseCase loadUserUseCase;

    public LoginResult loginWithCode(String code) {
        String kakaoAccessToken = kakaoAuthApiClient.exchangeCodeForToken(code);
        KakaoAuthApiClient.KakaoUserInfo userInfo = kakaoAuthApiClient.getUserInfo(kakaoAccessToken);

        Long kakaoId = userInfo.kakaoId();
        String kakaoIdStr = String.valueOf(kakaoId);

        boolean isUser = loadUserUseCase.isExist(kakaoId);
        String role = isUser ? loadUserUseCase.getUserRole(kakaoId).name() : null;

        String accessToken = jwtTokenProvider.createAccessToken(kakaoIdStr, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(kakaoIdStr);

        AuthController.UserData userData = isUser ? null : new AuthController.UserData(userInfo.nickname());

        return new LoginResult(accessToken, refreshToken, isUser, userData);
    }

    public record LoginResult(String accessToken, String refreshToken, boolean isUser, AuthController.UserData userData) {}
}