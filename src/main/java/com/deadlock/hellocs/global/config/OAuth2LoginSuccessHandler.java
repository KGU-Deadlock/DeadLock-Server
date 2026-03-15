package com.deadlock.hellocs.global.config;

import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.global.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final LoadUserUseCase loadUserUseCase;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long kakaoId = Long.valueOf(oAuth2User.getName());

        boolean isUser = loadUserUseCase.isExist(kakaoId);
        String role = isUser ? loadUserUseCase.getUserRole(kakaoId).name() : null;

        String accessToken = jwtTokenProvider.createAccessToken(authentication, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        Oauth2Response responseData = new Oauth2Response(accessToken, refreshToken, isUser);
        objectMapper.writeValue(response.getWriter(), responseData);
    }

    private record Oauth2Response(String accessToken, String refreshToken, boolean isUser) {}
}