package com.deadlock.hellocs.user.adapter.in.web.docs;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.common.web.resolver.CurrentUser;
import com.deadlock.hellocs.user.application.port.in.dto.ProfileResult;
import com.deadlock.hellocs.user.application.port.in.dto.UpdateMyInfoCommand;
import com.deadlock.hellocs.user.application.port.in.dto.UserProfileSummaryResult;
import com.deadlock.hellocs.user.application.port.in.dto.UserSignUpCommand;
import com.deadlock.hellocs.user.domain.UserLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "User", description = "사용자")
public interface UserControllerDocs {

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필 정보를 반환합니다.")
    ApiResponse<ProfileResult> getMyProfile(@Parameter(hidden = true) @CurrentUser Long userId);

    @Operation(summary = "회원가입", description = "카카오 OAuth2 로그인 후 최초 회원가입 시 호출합니다.")
    ApiResponse<Void> signup(@Parameter(hidden = true) @CurrentUser Long userId, UserSignUpCommand command);

    @Operation(summary = "내 정보 수정", description = "닉네임, 프로필 이미지, 관심 토픽을 부분 수정합니다.")
    ApiResponse<Void> updateMyInfo(@Parameter(hidden = true) @CurrentUser Long userId, UpdateMyInfoCommand command);

    @Operation(summary = "회원 탈퇴", description = "계정을 소프트 삭제합니다.")
    ApiResponse<Void> deleteMyAccount(@Parameter(hidden = true) @CurrentUser Long userId);

    @Operation(summary = "사용자 레벨 조회")
    ApiResponse<UserLevel> getUserLevel(Long kakaoId);

    @Operation(summary = "관심 토픽 ID 목록 조회")
    ApiResponse<List<Long>> getInterestTopicIds(Long kakaoId);

    @Operation(summary = "사용자 프로필 요약 조회", description = "내부 서비스 간 호출용 엔드포인트입니다.")
    ApiResponse<UserProfileSummaryResult> getProfileSummary(Long kakaoId);

    @Operation(summary = "사용자 프로필 요약 벌크 조회", description = "내부 서비스 간 호출용 엔드포인트입니다.")
    ApiResponse<List<UserProfileSummaryResult>> getProfileSummaries(List<Long> kakaoIds);
}
