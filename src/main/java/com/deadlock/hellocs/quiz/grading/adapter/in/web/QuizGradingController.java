package com.deadlock.hellocs.quiz.grading.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.quiz.grading.adapter.in.web.dto.SubmitAnswerResponse;
import com.deadlock.hellocs.quiz.grading.application.port.in.QueryGradingLogInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.SubmitAnswersCommand;
import com.deadlock.hellocs.quiz.grading.application.port.in.dto.UserGradingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz/grading")
@RequiredArgsConstructor
public class QuizGradingController {
    private final CommandAnswerInputPort commandAnswerInputPort;
    private final QueryGradingLogInputPort queryGradingLogInputPort;

    @PostMapping()
    public ApiResponse<SubmitAnswerResponse> submitAnswers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody List<UserGradingCommand> answers
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        String gradingLogId = commandAnswerInputPort.submit(new SubmitAnswersCommand(userId, answers));

        return ApiResponse.onSuccess(SubmitAnswerResponse.builder()
                .gradingLogId(gradingLogId)
                .build());
    }

    @GetMapping("/{gradingLogId}")
    public ApiResponse<GradingLogResult> getGradingLog(@PathVariable String gradingLogId) {
        return ApiResponse.onSuccess(
                queryGradingLogInputPort.getGradingLog(new GetGradingLogCommand(gradingLogId))
        );
    }

    @GetMapping("/{gradingLogId}/{quizId}")
    public ApiResponse<GradingDetailLogResult> getGradingDetailLog(@PathVariable String gradingLogId, @PathVariable Long quizId) {
        return ApiResponse.onSuccess(
                queryGradingLogInputPort.getGradingDetailLog(new GetGradingDetailLogCommand(gradingLogId, quizId))
        );
    }
}
