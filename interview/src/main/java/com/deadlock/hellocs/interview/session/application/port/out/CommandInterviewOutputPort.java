package com.deadlock.hellocs.interview.session.application.port.out;

import com.deadlock.hellocs.interview.session.domain.Interview;
import com.deadlock.hellocs.interview.session.domain.InterviewAnswer;
import com.deadlock.hellocs.interview.session.domain.InterviewQuestion;

import java.util.List;

public interface CommandInterviewOutputPort {
    Interview save(Interview interview);
    void saveQuestions(List<InterviewQuestion> questions);
    void saveAnswer(InterviewAnswer answer);
    void markCompleted(String interviewId);
}
