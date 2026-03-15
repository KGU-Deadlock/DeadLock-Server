package com.deadlock.hellocs.user.adapter.in.web;

import com.deadlock.hellocs.global.jwt.JwtTokenProvider;
import com.deadlock.hellocs.user.adapter.in.web.docs.UserControllerDocs;
import com.deadlock.hellocs.user.application.port.in.CreateUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.application.port.in.ManageUserUseCase;
import com.deadlock.hellocs.user.application.port.in.dto.ProfileResult;
import com.deadlock.hellocs.user.application.port.in.dto.UpdateMyInfoCommand;
import com.deadlock.hellocs.user.application.port.in.dto.UserSignUpCommand;
import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final CreateUserUseCase createUserUseCase;
    private final LoadUserUseCase loadUserUseCase;
    private final ManageUserUseCase manageUserUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public ApiResponse<Void> createUser(@RequestBody @Valid UserSignUpCommand command,
                                        @AuthenticationPrincipal Jwt jwtUser) {
        Long kakaoId = Long.valueOf(jwtUser.getSubject());
        createUserUseCase.createUser(kakaoId, command);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/me")
    public ApiResponse<ProfileResult> getMyProfile(@AuthenticationPrincipal Jwt jwtUser) {
        Long kakaoId = Long.valueOf(jwtUser.getSubject());
        return ApiResponse.onSuccess(loadUserUseCase.getProfile(kakaoId));
    }

    @PatchMapping("/me")
    public ApiResponse<Void> updateMyProfile(@RequestBody @Valid UpdateMyInfoCommand command,
                                             @AuthenticationPrincipal Jwt jwtUser) {
        Long kakaoId = Long.valueOf(jwtUser.getSubject());
        manageUserUseCase.updateMyInfo(kakaoId, command);
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyAccount(@AuthenticationPrincipal Jwt jwtUser) {
        Long kakaoId = Long.valueOf(jwtUser.getSubject());
        manageUserUseCase.deleteMyAccount(kakaoId);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/admin")
    public ApiResponse<AdminAccountTokenResponse> getAdminAccount() {
        Long kakaoId = 1L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(kakaoId.toString(), null);

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);
        boolean isUser = loadUserUseCase.isExist(kakaoId);

        return ApiResponse.onSuccess(new AdminAccountTokenResponse(accessToken, refreshToken, isUser));
    }

    private record AdminAccountTokenResponse(String accessToken, String refreshToken, boolean isUser) {}
}
