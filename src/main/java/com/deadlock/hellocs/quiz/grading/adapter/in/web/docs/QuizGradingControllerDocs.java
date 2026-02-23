package com.deadlock.hellocs.quiz.grading.adapter.in.web.docs;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.grading.adapter.in.web.dto.SubmitAnswerResponse;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Swagger/OpenAPI documentation contract for quiz grading endpoints.
 */
public interface QuizGradingControllerDocs {

    ApiResponse<SubmitAnswerResponse> submitAnswers(
            @AuthenticationPrincipal Jwt jwt,
            List<@Valid UserGradingCommand> answers
    );

    ApiResponse<GradingLogResult> getGradingLog(
            String gradingLogId
    );

    ApiResponse<GradingDetailLogResult> getGradingDetailLog(
            String gradingLogId,
            Long quizId
    );
}
