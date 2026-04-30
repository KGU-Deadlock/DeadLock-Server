package com.deadlock.hellocs.interview.feedback.application.port.out;

import com.deadlock.hellocs.interview.feedback.domain.InterviewFeedback;

public interface CommandFeedbackOutputPort {
    InterviewFeedback save(InterviewFeedback feedback);
}
