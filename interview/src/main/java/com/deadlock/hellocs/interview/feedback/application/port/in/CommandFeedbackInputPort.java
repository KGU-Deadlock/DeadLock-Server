package com.deadlock.hellocs.interview.feedback.application.port.in;

import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.GenerateFeedbackCommand;

public interface CommandFeedbackInputPort {
    FeedbackResult generateFeedback(GenerateFeedbackCommand command);
}
