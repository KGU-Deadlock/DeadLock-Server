package com.deadlock.hellocs.global.event;

import com.deadlock.hellocs.quiz.grading.application.event.GradingCompletedEvent;
import com.deadlock.hellocs.streak.adapter.in.event.StreakUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StreakEventBridge {

    private final ApplicationEventPublisher publisher;

    @EventListener
    public void onGradingCompleted(GradingCompletedEvent event) {
        if (event.solvedAt() == null) {
            return;
        }
        publisher.publishEvent(new StreakUpdateEvent(
                event.gradingLogId(),
                event.userId(),
                event.solvedAt().toLocalDate(),
                event.quizCount(),
                event.topicIds()
        ));
    }
}
