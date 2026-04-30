package com.deadlock.hellocs.interview.session.application.port.out;

import com.deadlock.hellocs.interview.session.domain.Interview;
import com.deadlock.hellocs.interview.session.domain.InterviewAnswer;
import com.deadlock.hellocs.interview.session.domain.InterviewQuestion;

import java.util.List;

public interface QueryInterviewOutputPort {
    Interview findById(String interviewId);
    List<InterviewQuestion> findQuestionsByInterviewId(String interviewId);
    List<InterviewAnswer> findAnswersByInterviewId(String interviewId);
}
