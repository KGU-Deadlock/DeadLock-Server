package com.deadlock.hellocs.interview.session.adapter.out.persistence;

import com.deadlock.hellocs.interview.session.adapter.out.persistence.entity.InterviewQuestionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestionJpaEntity, Long> {
    List<InterviewQuestionJpaEntity> findByInterviewIdOrderByQuestionNumber(String interviewId);
    Optional<InterviewQuestionJpaEntity> findByInterviewIdAndQuestionNumber(String interviewId, int questionNumber);
}
