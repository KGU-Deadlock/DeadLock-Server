package com.deadlock.hellocs.interview.session.adapter.out.persistence;

import com.deadlock.hellocs.interview.session.adapter.out.persistence.entity.InterviewJpaEntity;
import com.deadlock.hellocs.interview.session.domain.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InterviewRepository extends JpaRepository<InterviewJpaEntity, String> {

    @Modifying
    @Query("UPDATE InterviewJpaEntity i SET i.status = :status, i.completedAt = :completedAt WHERE i.id = :id")
    void updateStatusAndCompletedAt(
            @Param("id") String id,
            @Param("status") InterviewStatus status,
            @Param("completedAt") LocalDateTime completedAt
    );
}
