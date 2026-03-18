package com.deadlock.hellocs.global.auth.controller.docs;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.global.auth.controller.AuthController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
