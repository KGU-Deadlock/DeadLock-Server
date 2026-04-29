package com.deadlock.hellocs.interview.session.adapter.out.persistence;

import com.deadlock.hellocs.global.exception.CustomException;
import com.deadlock.hellocs.interview.exception.InterviewErrorStatus;
import com.deadlock.hellocs.interview.session.adapter.out.persistence.entity.InterviewAnswerJpaEntity;
import com.deadlock.hellocs.interview.session.adapter.out.persistence.entity.InterviewJpaEntity;
import com.deadlock.hellocs.interview.session.adapter.out.persistence.entity.InterviewQuestionJpaEntity;
import com.deadlock.hellocs.interview.session.application.port.out.CommandInterviewOutputPort;
import com.deadlock.hellocs.interview.session.application.port.out.QueryInterviewOutputPort;
import com.deadlock.hellocs.interview.session.domain.Interview;
import com.deadlock.hellocs.interview.session.domain.InterviewAnswer;
import com.deadlock.hellocs.interview.session.domain.InterviewQuestion;
import com.deadlock.hellocs.interview.session.domain.InterviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InterviewPersistenceAdapter implements CommandInterviewOutputPort, QueryInterviewOutputPort {

    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;

    @Override
    public Interview save(Interview interview) {
        return interviewRepository.save(InterviewJpaEntity.from(interview)).toDomain();
    }

    @Override
    public void saveQuestions(List<InterviewQuestion> questions) {
        List<InterviewQuestionJpaEntity> entities = questions.stream()
                .map(InterviewQuestionJpaEntity::from)
                .toList();
        interviewQuestionRepository.saveAll(entities);
    }

    @Override
    public void saveAnswer(InterviewAnswer answer) {
        interviewAnswerRepository.save(InterviewAnswerJpaEntity.from(answer));
    }

    @Override
    public void markCompleted(String interviewId) {
        interviewRepository.updateStatusAndCompletedAt(interviewId, InterviewStatus.COMPLETED, LocalDateTime.now());
    }

    @Override
    public Interview findById(String interviewId) {
        return interviewRepository.findById(interviewId)
                .map(InterviewJpaEntity::toDomain)
                .orElseThrow(() -> new CustomException(InterviewErrorStatus.INTERVIEW_NOT_FOUND));
    }

    @Override
    public List<InterviewQuestion> findQuestionsByInterviewId(String interviewId) {
        return interviewQuestionRepository
                .findByInterviewIdOrderByQuestionNumber(interviewId)
                .stream()
                .map(InterviewQuestionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<InterviewAnswer> findAnswersByInterviewId(String interviewId) {
        return interviewAnswerRepository
                .findByInterviewIdOrderByQuestionNumber(interviewId)
                .stream()
                .map(InterviewAnswerJpaEntity::toDomain)
                .toList();
    }
}
