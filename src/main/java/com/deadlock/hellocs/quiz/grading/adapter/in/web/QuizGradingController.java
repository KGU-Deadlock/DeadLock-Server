package com.deadlock.hellocs.quiz.grading.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.grading.adapter.in.web.docs.QuizGradingControllerDocs;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.quiz.grading.adapter.in.web.dto.SubmitAnswerResponse;
import com.deadlock.hellocs.quiz.grading.application.port.in.QueryGradingLogInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.SubmitAnswersCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz/grading")
@RequiredArgsConstructor
public class QuizGradingController implements QuizGradingControllerDocs {
    private final CommandAnswerInputPort commandAnswerInputPort;
    private final QueryGradingLogInputPort queryGradingLogInputPort;

    @PostMapping()
    @Override
    public ApiResponse<SubmitAnswerResponse> submitAnswers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody List<@Valid UserGradingCommand> answers
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        String gradingLogId = commandAnswerInputPort.submit(new SubmitAnswersCommand(userId, answers));

        return ApiResponse.onSuccess(SubmitAnswerResponse.builder()
                .gradingLogId(gradingLogId)
                .build());
    }

    @GetMapping("/{gradingLogId}")
    @Override
    public ApiResponse<GradingLogResult> getGradingLog(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String gradingLogId
    ) {
        Long requesterId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(
                queryGradingLogInputPort.getGradingLog(requesterId, new GetGradingLogCommand(gradingLogId))
        );
    }

    @GetMapping("/{gradingLogId}/{quizId}")
    @Override
    public ApiResponse<GradingDetailLogResult> getGradingDetailLog(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String gradingLogId,
            @PathVariable Long quizId
    ) {
        Long requesterId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(
                queryGradingLogInputPort.getGradingDetailLog(requesterId, new GetGradingDetailLogCommand(gradingLogId, quizId))
        );
    }
}
