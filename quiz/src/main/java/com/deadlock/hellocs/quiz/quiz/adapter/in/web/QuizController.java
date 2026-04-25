package com.deadlock.hellocs.quiz.quiz.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.quiz.adapter.in.web.docs.QuizControllerDocs;
import com.deadlock.hellocs.quiz.quiz.adapter.in.web.dto.GetQuizRequest;
import com.deadlock.hellocs.quiz.quiz.application.port.in.QueryQuizInputPort;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.quiz.application.port.in.dto.GetQuizResult;
import com.deadlock.hellocs.quiz.quiz.application.port.out.QueryUserOutputPort;
import com.deadlock.hellocs.quiz.shared.domain.QuizLevel;
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
    private final QueryUserOutputPort queryUserPort;

    @PostMapping()
    @Override
    public ApiResponse<GetQuizResult> getQuizzes(
            @RequestBody @Valid GetQuizRequest getQuizRequest,
            @AuthenticationPrincipal Jwt jwtUser
    ) {
        Long kakaoId = Long.valueOf(jwtUser.getSubject());
        QuizLevel userLevel = queryUserPort.getUserLevel(kakaoId);
        GetQuizCommand request = new GetQuizCommand(userLevel, getQuizRequest.topicIds(), getQuizRequest.mode());
        return ApiResponse.onSuccess(queryQuizInputPort.getQuizzes(request, kakaoId));
    }
}
