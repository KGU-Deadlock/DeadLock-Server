package com.deadlock.hellocs.interview.feedback.adapter.in.web.docs;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Interview Feedback", description = "면접 피드백 조회 API")
@SecurityRequirement(name = "bearerAuth")
public interface FeedbackControllerDocs {

    @Operation(summary = "피드백 조회", description = "완료된 면접의 AI 피드백을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "피드백 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드백 없음", content = @Content)
    })
    ApiResponse<FeedbackResult> getFeedback(
            @Parameter(name = "interviewId", description = "면접 세션 ID", required = true) String interviewId
    );
}
