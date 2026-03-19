package com.deadlock.hellocs.dev;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.global.auth.jwt.JwtTokenProvider;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/dev")
public class TestDataController {

    private final TestDataService testDataService;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoadUserUseCase loadUserUseCase;

    @PostMapping("/seed")
    public ApiResponse<SeedResult> seed() {
        return ApiResponse.onSuccess(testDataService.seed());
    }

    @GetMapping("/admin-token")
    public ApiResponse<AdminTokenResponse> getAdminToken() {
        Long kakaoId = 1L;
        String kakaoIdStr = String.valueOf(kakaoId);

        String accessToken = jwtTokenProvider.createAccessToken(kakaoIdStr, "ADMIN");
        String refreshToken = jwtTokenProvider.createRefreshToken(kakaoIdStr);
        boolean isUser = loadUserUseCase.isExist(kakaoId);

        return ApiResponse.onSuccess(new AdminTokenResponse(accessToken, refreshToken, isUser));
    }

    private record AdminTokenResponse(String accessToken, String refreshToken, boolean isUser) {}
}
