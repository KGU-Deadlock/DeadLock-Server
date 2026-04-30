package com.deadlock.hellocs.global.event;

import com.deadlock.hellocs.interview.session.application.event.InterviewCompletedEvent;
import com.deadlock.hellocs.streak.adapter.in.event.StreakUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InterviewStreakEventBridge {

    private final ApplicationEventPublisher publisher;

    @EventListener
    public void onInterviewCompleted(InterviewCompletedEvent event) {
        publisher.publishEvent(new StreakUpdateEvent(
                event.interviewId(),
                event.userId(),
                event.completedAt().toLocalDate(),
                event.questionCount(),
                List.of()
        ));
    }
}
