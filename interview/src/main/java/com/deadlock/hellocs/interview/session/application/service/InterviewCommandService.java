package com.deadlock.hellocs.interview.session.application.service;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.interview.exception.InterviewErrorStatus;
import com.deadlock.hellocs.interview.feedback.application.port.in.CommandFeedbackInputPort;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.GenerateFeedbackCommand;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.QaPair;
import com.deadlock.hellocs.interview.feedback.application.port.in.dto.FeedbackResult;
import com.deadlock.hellocs.interview.question.application.port.out.QueryCsQuestionOutputPort;
import com.deadlock.hellocs.interview.question.domain.CsQuestion;
import com.deadlock.hellocs.interview.session.application.event.InterviewCompletedEvent;
import com.deadlock.hellocs.interview.session.application.port.in.CommandInterviewInputPort;
import com.deadlock.hellocs.interview.session.application.port.in.dto.*;
import com.deadlock.hellocs.interview.session.application.port.out.CommandInterviewOutputPort;
import com.deadlock.hellocs.interview.session.application.port.out.QueryInterviewOutputPort;
import com.deadlock.hellocs.interview.session.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewCommandService implements CommandInterviewInputPort {

    private final CommandInterviewOutputPort commandInterviewOutputPort;
    private final QueryInterviewOutputPort queryInterviewOutputPort;
    private final QueryCsQuestionOutputPort queryCsQuestionOutputPort;
    private final CommandFeedbackInputPort commandFeedbackInputPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public StartInterviewResult startInterview(StartInterviewCommand command) {
        String interviewId = UUID.randomUUID().toString();

        Interview interview = Interview.builder()
                .id(interviewId)
                .userId(command.userId())
                .companyName(command.companyName())
                .position(command.position())
                .status(InterviewStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();
        commandInterviewOutputPort.save(interview);

        List<InterviewQuestion> questions = buildQuestions(interviewId, command.companyName());
        commandInterviewOutputPort.saveQuestions(questions);

        List<QuestionResult> questionResults = questions.stream()
                .map(q -> new QuestionResult(
                        q.getQuestionNumber(),
                        q.getQuestionText(),
                        q.getQuestionType().name().toLowerCase(),
                        q.getCategory()
                ))
                .toList();

        return new StartInterviewResult(interviewId, questionResults);
    }

    @Override
    public void submitAnswer(SubmitAnswerCommand command) {
        Interview interview = queryInterviewOutputPort.findById(command.interviewId());
        if (interview.isCompleted()) {
            throw new CustomException(InterviewErrorStatus.INTERVIEW_ALREADY_COMPLETED);
        }

        InterviewAnswer answer = InterviewAnswer.builder()
                .interviewId(command.interviewId())
                .questionNumber(command.questionNumber())
                .answerText(command.answerText())
                .durationSeconds(command.durationSeconds())
                .answeredAt(LocalDateTime.now())
                .build();

        commandInterviewOutputPort.saveAnswer(answer);
    }

    public FeedbackResult completeInterview(String interviewId) {
        Interview interview = queryInterviewOutputPort.findById(interviewId);
        if (interview.isCompleted()) {
            throw new CustomException(InterviewErrorStatus.INTERVIEW_ALREADY_COMPLETED);
        }

        List<InterviewQuestion> questions = queryInterviewOutputPort.findQuestionsByInterviewId(interviewId);
        List<InterviewAnswer> answers = queryInterviewOutputPort.findAnswersByInterviewId(interviewId);

        List<QaPair> qaPairs = buildQaPairs(questions, answers);

        LocalDateTime completedAt = LocalDateTime.now();
        commandInterviewOutputPort.markCompleted(interviewId);

        eventPublisher.publishEvent(new InterviewCompletedEvent(
                interviewId,
                interview.getUserId(),
                completedAt,
                questions.size()
        ));

        return commandFeedbackInputPort.generateFeedback(
                new GenerateFeedbackCommand(interviewId, interview.getCompanyName(), interview.getPosition(), qaPairs)
        );
    }

    private List<InterviewQuestion> buildQuestions(String interviewId, String companyName) {
        List<InterviewQuestion> questions = new ArrayList<>();

        questions.add(InterviewQuestion.builder()
                .interviewId(interviewId)
                .questionNumber(1)
                .questionText(companyName + "에 지원하게 된 동기가 무엇입니까?")
                .questionType(QuestionType.MOTIVATION)
                .build());

        questions.add(InterviewQuestion.builder()
                .interviewId(interviewId)
                .questionNumber(2)
                .questionText("만약 팀원과의 갈등이 생기면 어떻게 할 것입니까? 또한 본인도 그런 경험이 있었나요?")
                .questionType(QuestionType.BEHAVIORAL)
                .build());

        questions.add(InterviewQuestion.builder()
                .interviewId(interviewId)
                .questionNumber(3)
                .questionText("이 회사에서 경력을 쌓고 팀의 리더 자리에 올라갔을 때 어떤 리더가 되고 싶나요?")
                .questionType(QuestionType.LEADERSHIP)
                .build());

        List<CsQuestion> csQuestions = queryCsQuestionOutputPort.findRandomTwoWithoutDuplicate();
        questions.add(InterviewQuestion.builder()
                .interviewId(interviewId)
                .questionNumber(4)
                .questionText(csQuestions.get(0).getQuestion())
                .questionType(QuestionType.TECHNICAL)
                .category(csQuestions.get(0).getCategory().name())
                .build());

        questions.add(InterviewQuestion.builder()
                .interviewId(interviewId)
                .questionNumber(5)
                .questionText(csQuestions.get(1).getQuestion())
                .questionType(QuestionType.TECHNICAL)
                .category(csQuestions.get(1).getCategory().name())
                .build());

        return questions;
    }

    private List<QaPair> buildQaPairs(List<InterviewQuestion> questions, List<InterviewAnswer> answers) {
        return questions.stream()
                .map(q -> {
                    String answerText = answers.stream()
                            .filter(a -> a.getQuestionNumber() == q.getQuestionNumber())
                            .map(InterviewAnswer::getAnswerText)
                            .findFirst()
                            .orElse("");
                    return new QaPair(q.getQuestionText(), answerText, q.getQuestionType().name().toLowerCase());
                })
                .toList();
    }
}
