package com.deadlock.hellocs.interview.session.adapter.out.persistence.entity;

import com.deadlock.hellocs.interview.session.domain.Interview;
import com.deadlock.hellocs.interview.session.domain.InterviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewJpaEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public Interview toDomain() {
        return Interview.builder()
                .id(id)
                .userId(userId)
                .companyName(companyName)
                .position(position)
                .status(status)
                .createdAt(createdAt)
                .completedAt(completedAt)
                .build();
    }

    public static InterviewJpaEntity from(Interview interview) {
        return InterviewJpaEntity.builder()
                .id(interview.getId())
                .userId(interview.getUserId())
                .companyName(interview.getCompanyName())
                .position(interview.getPosition())
                .status(interview.getStatus())
                .createdAt(interview.getCreatedAt())
                .completedAt(interview.getCompletedAt())
                .build();
    }
}
