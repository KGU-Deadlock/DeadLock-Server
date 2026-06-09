package com.deadlock.hellocs.user.adapter.in.web;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.common.web.resolver.CurrentUser;
import com.deadlock.hellocs.user.adapter.in.web.docs.UserControllerDocs;
import com.deadlock.hellocs.user.application.port.in.CreateUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.application.port.in.ManageUserUseCase;
import com.deadlock.hellocs.user.application.port.in.dto.ProfileResult;
import com.deadlock.hellocs.user.application.port.in.dto.UpdateMyInfoCommand;
import com.deadlock.hellocs.user.application.port.in.dto.UserProfileSummaryResult;
import com.deadlock.hellocs.user.application.port.in.dto.UserSignUpCommand;
import com.deadlock.hellocs.user.domain.UserLevel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final CreateUserUseCase createUserUseCase;
    private final LoadUserUseCase loadUserUseCase;
    private final ManageUserUseCase manageUserUseCase;

    @GetMapping("/me")
    public ApiResponse<ProfileResult> getMyProfile(@CurrentUser Long userId) {
        return ApiResponse.onSuccess(loadUserUseCase.getProfile(userId));
    }

    @PostMapping("/signup")
    public ApiResponse<Void> signup(
            @CurrentUser Long userId,
            @RequestBody @Valid UserSignUpCommand command) {
        createUserUseCase.createUser(userId, command);
        return ApiResponse.onSuccess(null);
    }

    @PatchMapping("/me")
    public ApiResponse<Void> updateMyInfo(
            @CurrentUser Long userId,
            @RequestBody @Valid UpdateMyInfoCommand command) {
        manageUserUseCase.updateMyInfo(userId, command);
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyAccount(@CurrentUser Long userId) {
        manageUserUseCase.deleteMyAccount(userId);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/{kakaoId}/level")
    public ApiResponse<UserLevel> getUserLevel(@PathVariable Long kakaoId) {
        return ApiResponse.onSuccess(loadUserUseCase.getUserLevel(kakaoId));
    }

    @GetMapping("/{kakaoId}/interest-topic")
    public ApiResponse<List<Long>> getInterestTopicIds(@PathVariable Long kakaoId) {
        return ApiResponse.onSuccess(loadUserUseCase.getInterestTopicIds(kakaoId));
    }

    @GetMapping("/{kakaoId}/profile-summary")
    public ApiResponse<UserProfileSummaryResult> getProfileSummary(@PathVariable Long kakaoId) {
        return ApiResponse.onSuccess(loadUserUseCase.getProfileSummary(kakaoId));
    }

    @GetMapping("/profile-summaries")
    public ApiResponse<List<UserProfileSummaryResult>> getProfileSummaries(
            @RequestParam List<Long> kakaoIds) {
        return ApiResponse.onSuccess(loadUserUseCase.getProfileSummaries(kakaoIds));
    }
}
