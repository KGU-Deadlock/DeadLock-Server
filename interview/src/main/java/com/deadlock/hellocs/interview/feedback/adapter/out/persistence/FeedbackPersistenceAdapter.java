package com.deadlock.hellocs.interview.feedback.adapter.out.persistence;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.interview.exception.InterviewErrorStatus;
import com.deadlock.hellocs.interview.feedback.adapter.out.persistence.entity.InterviewFeedbackJpaEntity;
import com.deadlock.hellocs.interview.feedback.application.port.out.CommandFeedbackOutputPort;
import com.deadlock.hellocs.interview.feedback.application.port.out.QueryFeedbackOutputPort;
import com.deadlock.hellocs.interview.feedback.domain.InterviewFeedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedbackPersistenceAdapter implements CommandFeedbackOutputPort, QueryFeedbackOutputPort {

    private final FeedbackRepository feedbackRepository;

    @Override
    public InterviewFeedback save(InterviewFeedback feedback) {
        return feedbackRepository.save(InterviewFeedbackJpaEntity.from(feedback)).toDomain();
    }

    @Override
    public InterviewFeedback findByInterviewId(String interviewId) {
        return feedbackRepository.findByInterviewId(interviewId)
                .map(InterviewFeedbackJpaEntity::toDomain)
                .orElseThrow(() -> new CustomException(InterviewErrorStatus.INTERVIEW_FEEDBACK_NOT_FOUND));
    }
}
