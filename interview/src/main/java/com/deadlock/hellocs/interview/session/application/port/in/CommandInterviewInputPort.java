package com.deadlock.hellocs.interview.session.application.port.in;

import com.deadlock.hellocs.interview.session.application.port.in.dto.StartInterviewCommand;
import com.deadlock.hellocs.interview.session.application.port.in.dto.StartInterviewResult;
import com.deadlock.hellocs.interview.session.application.port.in.dto.SubmitAnswerCommand;

public interface CommandInterviewInputPort {
    StartInterviewResult startInterview(StartInterviewCommand command);
    void submitAnswer(SubmitAnswerCommand command);
}
