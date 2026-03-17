package com.deadlock.hellocs.global.auth.handler;

import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final LoadUserUseCase loadUserUseCase;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String kakaoIdStr = oAuth2User.getName();
        Long kakaoId = Long.valueOf(kakaoIdStr);

        boolean isUser = loadUserUseCase.isExist(kakaoId);
        String role = isUser ? loadUserUseCase.getUserRole(kakaoId).name() : null;

        String accessToken = jwtTokenProvider.createAccessToken(kakaoIdStr, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(kakaoIdStr);

        addRefreshTokenCookie(response, refreshToken);

        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(),
                new OAuth2SuccessResponse(accessToken, isUser));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth")
                .maxAge(Duration.ofMillis(jwtTokenProvider.getRefreshValidity()))
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private record OAuth2SuccessResponse(String accessToken, boolean isUser) {}
}
