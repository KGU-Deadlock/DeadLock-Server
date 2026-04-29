package com.deadlock.hellocs.interview.feedback.application.port.out;

import com.deadlock.hellocs.interview.feedback.domain.InterviewFeedback;

public interface QueryFeedbackOutputPort {
    InterviewFeedback findByInterviewId(String interviewId);
}
