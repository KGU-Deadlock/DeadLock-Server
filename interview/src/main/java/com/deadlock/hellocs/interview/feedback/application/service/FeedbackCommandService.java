package com.deadlock.hellocs.interview.feedback.application.service;

import com.deadlock.hellocs.interview.feedback.application.port.in.CommandFeedbackInputPort;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.GenerateFeedbackCommand;
import com.deadlock.hellocs.interview.feedback.application.port.out.CommandFeedbackOutputPort;
import com.deadlock.hellocs.interview.feedback.domain.InterviewFeedback;
import com.deadlock.hellocs.interview.feedback.domain.QuestionFeedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackCommandService implements CommandFeedbackInputPort {

    private final CommandFeedbackOutputPort commandFeedbackOutputPort;

    @Override
    public FeedbackResult generateFeedback(GenerateFeedbackCommand command) {
        List<QuestionFeedback> questionFeedbacks = command.questionFeedbacks();

        int overallScore = (int) Math.round(
                questionFeedbacks.stream().mapToInt(QuestionFeedback::getScore).average().orElse(0)
        );

        InterviewFeedback feedback = InterviewFeedback.builder()
                .interviewId(command.interviewId())
                .overallScore(overallScore)
                .questionFeedbacks(questionFeedbacks)
                .build();

        commandFeedbackOutputPort.save(feedback);

        return new FeedbackResult(overallScore, questionFeedbacks);
    }
}
