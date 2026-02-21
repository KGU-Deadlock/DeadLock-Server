package com.deadlock.hellocs.quiz.grading.adapter.in.web.docs;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.grading.adapter.in.web.dto.SubmitAnswerResponse;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@Tag(name = "Quiz Grading", description = "퀴즈 채점 API")
@SecurityRequirement(name = "bearerAuth")
public interface QuizGradingControllerDocs {

    @Operation(summary = "답안 제출", description = "사용자 답안을 제출하고 채점 로그 ID를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "답안 제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 검증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채점 대상 퀴즈를 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    ApiResponse<SubmitAnswerResponse> submitAnswers(
            @Parameter(hidden = true, description = "인증 사용자 JWT")
            Jwt jwt,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용자 답안 목록",
                    required = true
            )
            List<@Valid UserGradingCommand> answers
    );

    @Operation(summary = "채점 로그 조회", description = "채점 로그 ID로 전체 채점 결과를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채점 로그 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채점 로그를 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    ApiResponse<GradingLogResult> getGradingLog(
            @Parameter(description = "채점 로그 ID", required = true)
            String gradingLogId
    );

    @Operation(summary = "채점 상세 조회", description = "채점 로그 내 특정 퀴즈의 상세 채점 결과를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채점 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채점 상세를 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    ApiResponse<GradingDetailLogResult> getGradingDetailLog(
            @Parameter(description = "채점 로그 ID", required = true)
            String gradingLogId,
            @Parameter(description = "퀴즈 ID", required = true)
            Long quizId
    );
}
