package com.deadlock.hellocs.interview.session.adapter.in.web;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.common.web.resolver.CurrentUser;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;
import com.deadlock.hellocs.interview.session.adapter.in.web.docs.InterviewControllerDocs;
import com.deadlock.hellocs.interview.session.adapter.in.web.dto.StartInterviewRequest;
import com.deadlock.hellocs.interview.session.adapter.in.web.dto.SubmitAnswerRequest;
import com.deadlock.hellocs.interview.session.application.port.in.CommandInterviewInputPort;
import com.deadlock.hellocs.interview.session.application.port.in.dto.StartInterviewCommand;
import com.deadlock.hellocs.interview.session.application.port.in.dto.StartInterviewResult;
import com.deadlock.hellocs.interview.session.application.port.in.dto.SubmitAnswerCommand;
import com.deadlock.hellocs.interview.session.application.service.InterviewCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/interview")
@RequiredArgsConstructor
public class InterviewController implements InterviewControllerDocs {

    private final CommandInterviewInputPort commandInterviewInputPort;
    private final InterviewCommandService interviewCommandService;

    @PostMapping("/start")
    @Override
    public ApiResponse<StartInterviewResult> startInterview(
            @CurrentUser Long userId,
            @RequestBody @Valid StartInterviewRequest request
    ) {
        StartInterviewCommand command = new StartInterviewCommand(userId, request.companyName(), request.position());
        return ApiResponse.onSuccess(commandInterviewInputPort.startInterview(command));
    }

    @PostMapping("/{interviewId}/answer")
    @Override
    public ApiResponse<Void> submitAnswer(
            @PathVariable String interviewId,
            @RequestBody @Valid SubmitAnswerRequest request
    ) {
        SubmitAnswerCommand command = new SubmitAnswerCommand(
                interviewId,
                request.questionNumber(),
                request.answerText(),
                request.durationSeconds()
        );
        commandInterviewInputPort.submitAnswer(command);
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/{interviewId}/complete")
    @Override
    public ApiResponse<FeedbackResult> completeInterview(@PathVariable String interviewId) {
        return ApiResponse.onSuccess(interviewCommandService.completeInterview(interviewId));
    }
}
