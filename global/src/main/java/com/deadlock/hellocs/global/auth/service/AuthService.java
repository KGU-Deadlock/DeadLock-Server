package com.deadlock.hellocs.global.auth.service;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.auth.port.UserAuthPort;
import com.deadlock.hellocs.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserAuthPort userAuthPort;

    public TokenPair reissueTokens(String refreshToken) {
        Jwt decodedJwt = validateRefreshToken(refreshToken);
        String kakaoIdStr = decodedJwt.getSubject();
        Long kakaoId = Long.valueOf(kakaoIdStr);

        boolean isUser = userAuthPort.isExist(kakaoId);
        String role = isUser ? userAuthPort.getUserRoleName(kakaoId) : null;

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
