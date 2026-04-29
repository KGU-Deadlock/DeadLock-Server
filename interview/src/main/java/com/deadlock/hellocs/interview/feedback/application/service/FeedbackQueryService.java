package com.deadlock.hellocs.interview.feedback.application.service;

import com.deadlock.hellocs.interview.feedback.application.port.in.QueryFeedbackInputPort;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;
import com.deadlock.hellocs.interview.feedback.application.port.out.QueryFeedbackOutputPort;
import com.deadlock.hellocs.interview.feedback.domain.InterviewFeedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackQueryService implements QueryFeedbackInputPort {

    private final QueryFeedbackOutputPort queryFeedbackOutputPort;

    @Override
    public FeedbackResult getFeedback(String interviewId) {
        InterviewFeedback feedback = queryFeedbackOutputPort.findByInterviewId(interviewId);
        return new FeedbackResult(feedback.getOverallScore(), feedback.getQuestionFeedbacks());
    }
}
