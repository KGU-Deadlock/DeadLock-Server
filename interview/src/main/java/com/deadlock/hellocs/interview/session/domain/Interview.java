package com.deadlock.hellocs.interview.session.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class Interview {
    private String id;
    private Long userId;
    private String companyName;
    private String position;
    private InterviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<InterviewQuestion> questions;

    public boolean isCompleted() {
        return status == InterviewStatus.COMPLETED;
    }
}
