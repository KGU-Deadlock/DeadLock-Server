package com.deadlock.hellocs.interview.session.adapter.out.persistence;

import com.deadlock.hellocs.interview.session.adapter.out.persistence.entity.InterviewAnswerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswerJpaEntity, Long> {
    List<InterviewAnswerJpaEntity> findByInterviewIdOrderByQuestionNumber(String interviewId);
}
