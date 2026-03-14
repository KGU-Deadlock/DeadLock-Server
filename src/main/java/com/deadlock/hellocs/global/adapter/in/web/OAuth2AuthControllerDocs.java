package com.deadlock.hellocs.global.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/v1/auth")
public class OAuth2AuthControllerDocs {
//Docs 용 Class 이므로 실제 작동 로직은 없음(Spring Security 에서 가로챔)

    @Operation(
            summary = "카카오 OAuth2 로그인",
            description = """
                    카카오 로그인 페이지로 이동합니다.

                    **테스트 방법 (Swagger UI)**
                    1. 아래 **Try it out → Execute** 버튼 클릭
                    2. 브라우저에서 `http://localhost:8080/api/v1/auth/oauth2/kakao` 직접 접속
                    3. 카카오 계정으로 로그인
                    4. 로그인 성공 시 아래 형태의 JSON이 응답으로 반환됨
                    5. `accessToken` 값을 복사
                    6. Swagger UI 우측 상단 **Authorize 🔒** 버튼 클릭
                    7. `bearerAuth` 항목에 복사한 `accessToken` 붙여넣기 → **Authorize**
                    8. 이후 인증이 필요한 모든 API 테스트 가능
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "로그인 성공 — 응답 body에 JWT 포함",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = OAuth2LoginResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
                              "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
                              "isUser": true
                            }
                            """)
            )
    )
    @GetMapping("/oauth2/kakao")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/kakao");
    }

    @Schema(description = "OAuth2 로그인 성공 응답")
    record OAuth2LoginResponse(
            @Schema(description = "JWT 액세스 토큰 (API 호출 시 Authorization: Bearer {accessToken} 헤더에 사용)")
            String accessToken,
            @Schema(description = "JWT 리프레시 토큰 (액세스 토큰 만료 시 갱신용)")
            String refreshToken,
            @Schema(description = "기존 가입 유저 여부 (false = 신규, true = 기존 가입)")
            boolean isUser
    ) {}
}
