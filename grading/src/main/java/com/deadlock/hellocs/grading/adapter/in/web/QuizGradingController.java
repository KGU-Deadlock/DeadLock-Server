package com.deadlock.hellocs.grading.adapter.in.web;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.common.web.resolver.CurrentUser;
import com.deadlock.hellocs.grading.adapter.in.web.docs.QuizGradingControllerDocs;
import com.deadlock.hellocs.grading.adapter.in.web.dto.SubmitAnswerResponse;
import com.deadlock.hellocs.grading.application.port.in.CommandAnswerInputPort;
import com.deadlock.hellocs.grading.application.port.in.QueryGradingLogInputPort;
import com.deadlock.hellocs.grading.application.port.in.dto.GetGradingDetailLogCommand;
import com.deadlock.hellocs.grading.application.port.in.dto.GetGradingLogCommand;
import com.deadlock.hellocs.grading.application.port.in.dto.GradingDetailLogResult;
import com.deadlock.hellocs.grading.application.port.in.dto.GradingLogListResult;
import com.deadlock.hellocs.grading.application.port.in.dto.GradingLogResult;
import com.deadlock.hellocs.grading.application.port.in.dto.SubmitAnswersCommand;
import com.deadlock.hellocs.grading.application.port.in.dto.UserGradingCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/quiz/grading")
@RequiredArgsConstructor
public class QuizGradingController implements QuizGradingControllerDocs {
    private final CommandAnswerInputPort commandAnswerInputPort;
    private final QueryGradingLogInputPort queryGradingLogInputPort;

    @PostMapping()
    @Override
    public ApiResponse<SubmitAnswerResponse> submitAnswers(
            @CurrentUser Long userId,
            @RequestBody List<@Valid UserGradingCommand> answers
    ) {
        String gradingLogId = commandAnswerInputPort.submit(new SubmitAnswersCommand(userId, answers));

        return ApiResponse.onSuccess(SubmitAnswerResponse.builder()
                .gradingLogId(gradingLogId)
                .build());
    }

    @GetMapping("/{gradingLogId}")
    @Override
    public ApiResponse<GradingLogResult> getGradingLog(
            @CurrentUser Long requesterId,
            @PathVariable("gradingLogId") String gradingLogId
    ) {
        return ApiResponse.onSuccess(
                queryGradingLogInputPort.getGradingLog(requesterId, new GetGradingLogCommand(gradingLogId))
        );
    }

    @GetMapping("/{gradingLogId}/{quizId}")
    @Override
    public ApiResponse<GradingDetailLogResult> getGradingDetailLog(
            @CurrentUser Long requesterId,
            @PathVariable("gradingLogId") String gradingLogId,
            @PathVariable("quizId") Long quizId
    ) {
        return ApiResponse.onSuccess(
                queryGradingLogInputPort.getGradingDetailLog(requesterId, new GetGradingDetailLogCommand(gradingLogId, quizId))
        );
    }

    @GetMapping("/list")
    @Override
    public ApiResponse<List<GradingLogListResult>> getGradingLogList(
            @CurrentUser Long requesterId
    ) {
        return ApiResponse.onSuccess(
                queryGradingLogInputPort.getGradingLogList(requesterId)
        );
    }
}
