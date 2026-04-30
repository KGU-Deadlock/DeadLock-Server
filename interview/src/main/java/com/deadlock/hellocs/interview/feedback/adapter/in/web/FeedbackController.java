package com.deadlock.hellocs.interview.feedback.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.interview.feedback.adapter.in.web.docs.FeedbackControllerDocs;
import com.deadlock.hellocs.interview.feedback.application.port.in.QueryFeedbackInputPort;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/interview")
@RequiredArgsConstructor
public class FeedbackController implements FeedbackControllerDocs {

    private final QueryFeedbackInputPort queryFeedbackInputPort;

    @GetMapping("/{interviewId}/feedback")
    @Override
    public ApiResponse<FeedbackResult> getFeedback(@PathVariable String interviewId) {
        return ApiResponse.onSuccess(queryFeedbackInputPort.getFeedback(interviewId));
    }
}
