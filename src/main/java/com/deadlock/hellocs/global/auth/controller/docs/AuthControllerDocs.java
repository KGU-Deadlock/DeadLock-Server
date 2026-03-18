package com.deadlock.hellocs.global.auth.controller.docs;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.global.auth.controller.AuthController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "카카오 인가 코드로 로그인",
            description = "카카오 인가 코드(code)를 받아 토큰 교환 → 유저 정보 조회 → JWT 발급을 자동 처리합니다. "
                    + "Spring Security OAuth2가 내부적으로 처리하는 엔드포인트입니다. "
                    + "성공 시 accessToken과 isUser(회원가입 여부)를 응답 body로, refreshToken은 HttpOnly 쿠키로 전달합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthController.OAuth2TokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 인가 코드",
                    content = @Content
            )
    })
    void kakaoCallback(
            @Parameter(description = "카카오 인가 코드", required = true, example = "abc123")
            String code,
            @Parameter(description = "CSRF 방지용 상태값", required = true)
            String state
    );

    @Operation(summary = "토큰 재발급",
            description = "refreshToken 쿠키를 사용하여 새로운 accessToken을 발급받습니다. "
                    + "refreshToken도 함께 갱신됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재발급 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 리프레시 토큰(AUTH401)",
                    content = @Content
            )
    })
    ApiResponse<AuthController.ReissueResponse> reissue(String refreshToken,
                                                         HttpServletResponse response);
}
