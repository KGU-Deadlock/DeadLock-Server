package com.deadlock.hellocs.quiz.quiz.adapter.in.web.docs;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto.GetQuizRequest;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@Tag(name = "Quiz", description = "퀴즈 조회 API")
@SecurityRequirement(name = "bearerAuth")
public interface QuizControllerDocs {

    @Operation(summary = "퀴즈 목록 조회", description = "사용자 레벨과 요청 조건으로 퀴즈 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "퀴즈 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "퀴즈 요청 검증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    ApiResponse<List<Quiz>> getQuizzes(
            @ParameterObject
            @Parameter(description = "퀴즈 조회 필터(주제 ID 목록, 문제 모드)")
            GetQuizRequest getQuizRequest,
            @Parameter(hidden = true, description = "인증 사용자 JWT")
            Jwt jwtUser
    );
}
