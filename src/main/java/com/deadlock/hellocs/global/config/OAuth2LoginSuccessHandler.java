package com.deadlock.hellocs.global.config;

import com.deadlock.hellocs.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long kakaoId = Long.valueOf(oAuth2User.getName());

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        Oauth2Response responseData = new Oauth2Response(accessToken, refreshToken, false);
        if(userRepository.findByKakaoId(kakaoId).isPresent()){
            responseData = new Oauth2Response(accessToken, refreshToken, true);
        }
        objectMapper.writeValue(response.getWriter(), responseData);
    }

    private record Oauth2Response(String accessToken, String refreshToken, boolean isUser) {}
}