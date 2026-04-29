package com.deadlock.hellocs.interview.feedback.application.port.out;

import com.deadlock.hellocs.interview.feedback.application.port.out.dto.AiFeedbackRequest;
import com.deadlock.hellocs.interview.feedback.application.port.out.dto.AiFeedbackResponse;

public interface AiFeedbackOutputPort {
    AiFeedbackResponse evaluate(AiFeedbackRequest request);
}
