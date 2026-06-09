package com.deadlock.hellocs.user.auth.controller;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.user.application.port.in.dto.LoginUserData;
import com.deadlock.hellocs.user.auth.controller.docs.AuthControllerDocs;
import com.deadlock.hellocs.user.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.user.auth.service.AuthService;
import com.deadlock.hellocs.user.auth.service.KakaoCodeAuthService;
import com.deadlock.hellocs.user.auth.util.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
        CookieUtils.addRefreshTokenCookie(response, tokenPair.refreshToken(), jwtTokenProvider.getRefreshValidity());

        return ApiResponse.onSuccess(new AuthTokenResponse(tokenPair.accessToken(), tokenPair.isUser(), null));
    }

    @PostMapping("/kakao/code")
    public ApiResponse<AuthTokenResponse> loginWithKakaoCode(
            @RequestBody @Valid KakaoCodeRequest request,
            HttpServletResponse response) {
        KakaoCodeAuthService.LoginResult result = kakaoCodeAuthService.loginWithCode(request.code());
        CookieUtils.addRefreshTokenCookie(response, result.refreshToken(), jwtTokenProvider.getRefreshValidity());
        return ApiResponse.onSuccess(new AuthTokenResponse(result.accessToken(), result.isUser(), result.userData()));
    }

    public record AuthTokenResponse(String accessToken, boolean isUser, LoginUserData userData) {}

    public record KakaoCodeRequest(@NotBlank String code) {}
}
