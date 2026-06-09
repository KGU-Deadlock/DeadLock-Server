package com.deadlock.hellocs.interview.session.adapter.in.web.docs;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.common.web.resolver.CurrentUser;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;
import com.deadlock.hellocs.interview.session.adapter.in.web.dto.StartInterviewRequest;
import com.deadlock.hellocs.interview.session.adapter.in.web.dto.SubmitAnswerRequest;
import com.deadlock.hellocs.interview.session.application.port.in.dto.StartInterviewResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Interview", description = "화상 면접 연습 API")
@SecurityRequirement(name = "bearerAuth")
public interface InterviewControllerDocs {

    @Operation(summary = "면접 시작", description = "회사명과 직무를 입력받아 면접 세션을 생성하고 5개의 질문을 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "면접 세션 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "CS 질문 부족 또는 서버 오류", content = @Content)
    })
    ApiResponse<StartInterviewResult> startInterview(
            @Parameter(hidden = true, description = "인증 사용자 ID") @CurrentUser Long userId,
            StartInterviewRequest request
    );

    @Operation(summary = "답변 제출", description = "각 질문에 대한 답변을 제출합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "답변 제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 완료된 면접 또는 요청 검증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "면접 세션 없음", content = @Content)
    })
    ApiResponse<Void> submitAnswer(
            @Parameter(name = "interviewId", description = "면접 세션 ID", required = true) String interviewId,
            SubmitAnswerRequest request
    );

    @Operation(summary = "면접 완료 및 AI 피드백 생성", description = "면접을 완료하고 AI 피드백을 생성하여 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "피드백 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 완료된 면접", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "면접 세션 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI 피드백 생성 실패", content = @Content)
    })
    ApiResponse<FeedbackResult> completeInterview(
            @Parameter(name = "interviewId", description = "면접 세션 ID", required = true) String interviewId
    );
}
