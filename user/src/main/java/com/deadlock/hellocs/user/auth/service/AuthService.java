package com.deadlock.hellocs.user.auth.service;

import com.deadlock.hellocs.common.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.common.exception.CustomException;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.auth.jwt.JwtTokenProvider;
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
        Long kakaoId = Long.valueOf(kakaoIdStr);

        boolean isUser = loadUserUseCase.isExist(kakaoId);
        String role = isUser ? loadUserUseCase.getUserRole(kakaoId).name() : null;

        return new TokenPair(
                jwtTokenProvider.createAccessToken(kakaoIdStr, role),
                jwtTokenProvider.createRefreshToken(kakaoIdStr),
                isUser
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

    public record TokenPair(String accessToken, String refreshToken, boolean isUser) {}
}
