package com.deadlock.hellocs.quiz.adapter.in.web;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.common.web.resolver.CurrentUser;
import com.deadlock.hellocs.quiz.adapter.in.web.docs.QuizControllerDocs;
import com.deadlock.hellocs.quiz.adapter.in.web.dto.GetQuizRequest;
import com.deadlock.hellocs.quiz.application.port.in.QueryQuizInputPort;
import com.deadlock.hellocs.quiz.application.port.in.dto.GetQuizCommand;
import com.deadlock.hellocs.quiz.application.port.in.dto.GetQuizResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/quiz")
@RequiredArgsConstructor
public class QuizController implements QuizControllerDocs {

    private final QueryQuizInputPort queryQuizInputPort;

    @PostMapping()
    @Override
    public ApiResponse<GetQuizResult> getQuizzes(
            @RequestBody @Valid GetQuizRequest getQuizRequest,
            @CurrentUser Long userId
    ) {
        GetQuizCommand request = new GetQuizCommand(getQuizRequest.topicIds(), getQuizRequest.mode());
        return ApiResponse.onSuccess(queryQuizInputPort.getQuizzes(request, userId));
    }
}
