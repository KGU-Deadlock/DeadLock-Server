package com.deadlock.hellocs.interview.feedback.application.port.in;

import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;

public interface QueryFeedbackInputPort {
    FeedbackResult getFeedback(String interviewId);
}
