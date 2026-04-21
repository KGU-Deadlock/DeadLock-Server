package com.deadlock.hellocs.global.event;

import com.deadlock.hellocs.quiz.grading.application.event.GradingCompletedEvent;
import com.deadlock.hellocs.ranking.adapter.in.event.QuizCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingEventBridge {

    private final ApplicationEventPublisher publisher;

    @EventListener
    public void onGradingCompleted(GradingCompletedEvent event) {
        publisher.publishEvent(new QuizCompletedEvent(
                event.userId(),
                event.topicIds(),
                event.totalScore()
        ));
    }
}
