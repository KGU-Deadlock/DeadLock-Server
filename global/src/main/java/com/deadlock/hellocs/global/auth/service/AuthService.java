package com.deadlock.hellocs.global.auth.service;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.auth.client.KakaoApiClient;
import com.deadlock.hellocs.global.auth.client.KakaoApiClient.KakaoUserInfo;
import com.deadlock.hellocs.global.auth.controller.AuthController.UserData;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoadUserUseCase loadUserUseCase;
    private final KakaoApiClient kakaoApiClient;

    public TokenPair loginWithKakaoAccessToken(String kakaoAccessToken) {
        kakaoApiClient.validateToken(kakaoAccessToken);
        KakaoUserInfo userInfo = kakaoApiClient.getUserInfo(kakaoAccessToken);

        String kakaoIdStr = String.valueOf(userInfo.id());
        Long kakaoId = userInfo.id();

        boolean isUser = loadUserUseCase.isExist(kakaoId);
        String role = isUser ? loadUserUseCase.getUserRole(kakaoId).name() : null;

        UserData userData = null;
        if (!isUser && userInfo.kakaoAccount() != null && userInfo.kakaoAccount().profile() != null) {
            userData = new UserData(userInfo.kakaoAccount().profile().nickname());
        }

        return new TokenPair(
                jwtTokenProvider.createAccessToken(kakaoIdStr, role),
                jwtTokenProvider.createRefreshToken(kakaoIdStr),
                isUser,
                userData
        );
    }

    public TokenPair reissueTokens(String refreshToken) {
        Jwt decodedJwt = validateRefreshToken(refreshToken);
        String kakaoIdStr = decodedJwt.getSubject();
        Long kakaoId = Long.valueOf(kakaoIdStr);

        boolean isUser = loadUserUseCase.isExist(kakaoId);
        String role = isUser ? loadUserUseCase.getUserRole(kakaoId).name() : null;

        return new TokenPair(
                jwtTokenProvider.createAccessToken(kakaoIdStr, role),
                jwtTokenProvider.createRefreshToken(kakaoIdStr),
                isUser,
                null
        );
    }

    private Jwt validateRefreshToken(String refreshToken) {
        Jwt decodedJwt = decodeToken(refreshToken);

        String type = decodedJwt.getClaimAsString(JwtTokenProvider.CLAIM_TYPE);
        if (!JwtTokenProvider.TYPE_REFRESH.equals(type)) {
            throw new CustomException(ErrorStatus._INVALID_REFRESH_TOKEN);
        }

        return decodedJwt;
    }

    private Jwt decodeToken(String token) {
        try {
            return jwtTokenProvider.baseJwtDecoder().decode(token);
        } catch (JwtException e) {
            throw new CustomException(ErrorStatus._INVALID_REFRESH_TOKEN);
        }
    }

    public record TokenPair(String accessToken, String refreshToken, boolean isUser, UserData userData) {}
}
