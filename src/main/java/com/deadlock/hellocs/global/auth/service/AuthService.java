package com.deadlock.hellocs.global.auth.service;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
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

    public TokenPair reissueTokens(String refreshToken) {
        Jwt decodedJwt = validateRefreshToken(refreshToken);
        String kakaoIdStr = decodedJwt.getSubject();
        String role = getUserRole(Long.valueOf(kakaoIdStr));

        return new TokenPair(
                jwtTokenProvider.createAccessToken(kakaoIdStr, role),
                jwtTokenProvider.createRefreshToken(kakaoIdStr)
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

    private String getUserRole(Long kakaoId) {
        if (!loadUserUseCase.isExist(kakaoId)) {
            throw new CustomException(ErrorStatus._USER_NOT_FOUND);
        }
        return loadUserUseCase.getUserRole(kakaoId).name();
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
