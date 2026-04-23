package com.deadlock.hellocs.quiz.quiz.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.quiz.adapter.in.web.docs.QuizControllerDocs;
import com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto.GetQuizRequest;
import com.deadlock.hellocs.quiz.quiz.application.port.in.QueryQuizInputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizResult;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
// TODO: user 모듈이 별도 서브모듈로 분리되면 project(':user') 의존성 추가 및 컴파일 에러 해소 필요
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/quiz")
@RequiredArgsConstructor
public class QuizController implements QuizControllerDocs {
    private final QueryQuizInputPort queryQuizInputPort;
    private final LoadUserUseCase loadUserUseCase;

    @PostMapping()
    @Override
    public ApiResponse<GetQuizResult> getQuizzes(
            @RequestBody @Valid GetQuizRequest getQuizRequest,
            @AuthenticationPrincipal Jwt jwtUser
    ) {
        Long kakaoId = Long.valueOf(jwtUser.getSubject());
        QuizLevel userLevel = loadUserUseCase.getUserLevel(kakaoId);
        GetQuizCommand request = new GetQuizCommand(userLevel, getQuizRequest.topicIds(), getQuizRequest.mode());
        return ApiResponse.onSuccess(queryQuizInputPort.getQuizzes(request, kakaoId));
    }
}
