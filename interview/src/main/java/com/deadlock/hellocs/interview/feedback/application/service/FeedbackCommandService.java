package com.deadlock.hellocs.interview.feedback.application.service;

import com.deadlock.hellocs.interview.feedback.application.port.in.CommandFeedbackInputPort;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.GenerateFeedbackCommand;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.QaPair;
import com.deadlock.hellocs.interview.feedback.application.port.out.AiFeedbackOutputPort;
import com.deadlock.hellocs.interview.feedback.application.port.out.CommandFeedbackOutputPort;
import com.deadlock.hellocs.interview.feedback.application.port.out.dto.AiFeedbackRequest;
import com.deadlock.hellocs.interview.feedback.application.port.out.dto.AiFeedbackResponse;
import com.deadlock.hellocs.interview.feedback.domain.InterviewFeedback;
import com.deadlock.hellocs.interview.feedback.domain.QuestionFeedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackCommandService implements CommandFeedbackInputPort {

    private final AiFeedbackOutputPort aiFeedbackOutputPort;
    private final CommandFeedbackOutputPort commandFeedbackOutputPort;

    @Override
    public FeedbackResult generateFeedback(GenerateFeedbackCommand command) {
        List<QuestionFeedback> questionFeedbacks = evaluateEachQuestion(command.qaPairs());

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

    private List<QuestionFeedback> evaluateEachQuestion(List<QaPair> qaPairs) {
        List<QuestionFeedback> results = new ArrayList<>();
        for (int i = 0; i < qaPairs.size(); i++) {
            QaPair qa = qaPairs.get(i);
            AiFeedbackRequest request = new AiFeedbackRequest(qa.question(), qa.answer(), null);
            AiFeedbackResponse response = aiFeedbackOutputPort.evaluate(request);
            results.add(QuestionFeedback.builder()
                    .questionNumber(i + 1)
                    .score(response.score())
                    .missingKeywords(response.missingKeywords())
                    .improvedAnswer(response.improvedAnswer())
                    .message(response.message())
                    .build());
        }
        return results;
    }
}
