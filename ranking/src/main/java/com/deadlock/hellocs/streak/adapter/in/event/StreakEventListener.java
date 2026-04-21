package com.deadlock.hellocs.streak.adapter.in.event;

import com.deadlock.hellocs.streak.application.port.in.RecordStreakUseCase;
import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreakEventListener {

    private final RecordStreakUseCase recordStreakUseCase;

    @EventListener
    public void handle(StreakUpdateEvent event) {
        try {
            recordStreakUseCase.record(new RecordStreakCommand(
                    event.gradingLogId(),
                    event.userId(),
                    event.solvedDate(),
                    event.quizCount(),
                    event.topicIds()
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update streak. gradingLogId={}", event.gradingLogId(), e);
        }
    }
}