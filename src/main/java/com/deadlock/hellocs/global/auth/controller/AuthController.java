package com.deadlock.hellocs.global.auth.controller;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.global.auth.controller.docs.AuthControllerDocs;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.global.auth.service.AuthService;
import com.deadlock.hellocs.global.auth.service.KakaoCodeAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoCodeAuthService kakaoCodeAuthService;

    @GetMapping("/oauth2/kakao")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/kakao");
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

    @PostMapping("/kakao/code")
    public ApiResponse<AuthTokenResponse> loginWithKakaoCode(
            @RequestBody @Valid KakaoCodeRequest request,
            HttpServletResponse response) {
        KakaoCodeAuthService.LoginResult result = kakaoCodeAuthService.loginWithCode(request.code());
        addRefreshTokenCookie(response, result.refreshToken());
        return ApiResponse.onSuccess(new AuthTokenResponse(result.accessToken(), result.isUser(), result.userData()));
    }

    @GetMapping("/token")
    public void kakaoCallback(@RequestParam String code, @RequestParam String state) {
        // Spring Security OAuth2가 실제로 처리하므로 이 메서드는 호출되지 않습니다.
        // Swagger 명세 전용 엔드포인트입니다.
    }

    public record AuthTokenResponse(String accessToken, boolean isUser, UserData userData) {}

    public record UserData(String nickname) {}

    public record KakaoCodeRequest(@NotBlank String code) {}
}
