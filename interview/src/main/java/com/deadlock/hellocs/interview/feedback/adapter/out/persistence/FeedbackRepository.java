package com.deadlock.hellocs.interview.feedback.adapter.out.persistence;

import com.deadlock.hellocs.interview.feedback.adapter.out.persistence.entity.InterviewFeedbackJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<InterviewFeedbackJpaEntity, Long> {
    Optional<InterviewFeedbackJpaEntity> findByInterviewId(String interviewId);
}
