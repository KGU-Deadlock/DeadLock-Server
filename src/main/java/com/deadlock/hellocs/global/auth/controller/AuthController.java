package com.deadlock.hellocs.global.auth.controller;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.global.auth.controller.docs.AuthControllerDocs;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/kakao")
    public ApiResponse<AuthTokenResponse> kakaoTokenLogin(
            @RequestBody KakaoTokenRequest request,
            HttpServletResponse response) {

        AuthService.TokenPair tokenPair = authService.loginWithKakaoAccessToken(request.accessToken());
        addRefreshTokenCookie(response, tokenPair.refreshToken());

        return ApiResponse.onSuccess(
                new AuthTokenResponse(tokenPair.accessToken(), tokenPair.isUser(), tokenPair.userData()));
    }

    @PostMapping("/reissue")
    public ApiResponse<AuthTokenResponse> reissue(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response) {

        AuthService.TokenPair tokenPair = authService.reissueTokens(refreshToken);
        addRefreshTokenCookie(response, tokenPair.refreshToken());

        return ApiResponse.onSuccess(new AuthTokenResponse(tokenPair.accessToken(), tokenPair.isUser(), null));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMillis(jwtTokenProvider.getRefreshValidity()))
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record KakaoTokenRequest(String accessToken) {}

    public record AuthTokenResponse(String accessToken, boolean isUser, UserData userData) {}

    public record UserData(String nickname) {}
}
