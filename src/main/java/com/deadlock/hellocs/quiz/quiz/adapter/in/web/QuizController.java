package com.deadlock.hellocs.quiz.quiz.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto.GetQuizRequest;
import com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto.GetQuizResponse;
import com.deadlock.hellocs.quiz.quiz.application.port.in.QueryQuizInputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
public class QuizController {
    private final QueryQuizInputPort queryQuizInputPort;
    private final LoadUserUseCase loadUserUseCase;

    @GetMapping()
    public ApiResponse<List<GetQuizResponse>> getQuizzes(
            GetQuizRequest getQuizRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long kakaoId = Long.valueOf(jwt.getSubject());
        QuizLevel userLevel = loadUserUseCase.getUserLevel(kakaoId);
        GetQuizCommand request = new GetQuizCommand(userLevel, getQuizRequest.topicIds(), getQuizRequest.mode());
        return ApiResponse.onSuccess(queryQuizInputPort.getQuizzes(request).stream()
                .map(GetQuizResponse::from)
                .toList());
    }
}
