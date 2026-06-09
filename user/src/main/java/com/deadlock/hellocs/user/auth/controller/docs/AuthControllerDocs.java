package com.deadlock.hellocs.user.auth.controller.docs;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.user.auth.controller.AuthController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Tag(name = "Auth", description = "인증")
public interface AuthControllerDocs {

    @Operation(summary = "카카오 OAuth2 로그인",
            description = "카카오 OAuth2 인증 페이지로 리다이렉트합니다. "
                    + "인증 성공 시 accessToken은 응답 body, refreshToken은 HttpOnly 쿠키로 전달됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "카카오 로그인 페이지로 리다이렉트"
            )
    })
    void kakaoLogin(HttpServletResponse response) throws IOException;

    @Operation(summary = "프론트 카카오 인가 코드로 로그인",
            description = "프론트엔드에서 카카오 OAuth2 로그인 후 받은 인가 코드(code)만으로 JWT를 발급받습니다. "
                    + "성공 시 accessToken과 isUser(회원가입 여부)를 응답 body로, refreshToken은 HttpOnly 쿠키로 전달합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthController.AuthTokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 인가 코드(AUTH400)",
                    content = @Content
            )
    })
    ApiResponse<AuthController.AuthTokenResponse> loginWithKakaoCode(
            AuthController.KakaoCodeRequest request,
            HttpServletResponse response
    );

    @Operation(summary = "토큰 재발급",
            description = "refreshToken 쿠키를 사용하여 새로운 accessToken을 발급받습니다. "
                    + "성공 시 새로운 accessToken이 응답 body에, 새로운 refreshToken이 HttpOnly 쿠키로 전달됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = AuthController.AuthTokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 리프레시 토큰(AUTH401)",
                    content = @Content
            )
    })
    ApiResponse<AuthController.AuthTokenResponse> reissue(String refreshToken,
                                                          HttpServletResponse response);
}
