package com.deadlock.hellocs.global.auth.controller.docs;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.global.auth.controller.AuthController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "Auth", description = "인증")
public interface AuthControllerDocs {

    @Operation(summary = "카카오 액세스 토큰으로 로그인",
            description = "프론트엔드에서 카카오 SDK로 발급받은 액세스 토큰을 전달하면 "
                    + "백엔드가 카카오 API로 유저 정보를 조회하여 hellocs JWT를 발급합니다. "
                    + "성공 시 accessToken과 isUser(기가입 여부)를 응답 body로, refreshToken은 HttpOnly 쿠키로 전달합니다. "
                    + "신규 유저인 경우 userData(nickname)가 함께 반환됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthController.AuthTokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 카카오 액세스 토큰 (AUTH402)",
                    content = @Content
            )
    })
    ApiResponse<AuthController.AuthTokenResponse> kakaoTokenLogin(
            AuthController.KakaoTokenRequest request,
            HttpServletResponse response);

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
                    description = "유효하지 않은 리프레시 토큰 (AUTH401)",
                    content = @Content
            )
    })
    ApiResponse<AuthController.AuthTokenResponse> reissue(String refreshToken, HttpServletResponse response);
}
