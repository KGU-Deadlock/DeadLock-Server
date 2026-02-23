package com.deadlock.hellocs.quiz.quiz.adapter.in.web.docs;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto.GetQuizRequest;
import com.deadlock.hellocs.quiz.quiz.domain.Quiz;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Swagger/OpenAPI documentation contract for quiz endpoints.
 */
public interface QuizControllerDocs {

    ApiResponse<List<Quiz>> getQuizzes(
            GetQuizRequest getQuizRequest,
            @AuthenticationPrincipal Jwt jwtUser
    );
}
