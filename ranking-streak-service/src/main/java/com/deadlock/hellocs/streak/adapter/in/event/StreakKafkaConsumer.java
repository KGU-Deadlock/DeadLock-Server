package com.deadlock.hellocs.streak.adapter.in.event;

import com.deadlock.hellocs.shared.events.GradingCompletedEventMessage;
import com.deadlock.hellocs.streak.application.port.in.RecordStreakInputPort;
import com.deadlock.hellocs.streak.application.port.in.dto.RecordStreakCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreakKafkaConsumer {

    private final RecordStreakInputPort recordStreakInputPort;

    @KafkaListener(
            topics = "grading.completed",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(GradingCompletedEventMessage event) {
        try {
            if (event == null || event.gradingLogId() == null
                    || event.solvedAt() == null || event.userId() == null) {
                return;
            }
            recordStreakInputPort.record(new RecordStreakCommand(
                    event.gradingLogId(),
                    event.userId(),
                    event.solvedAt().toLocalDate(),
                    event.quizCount()
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update streak. gradingLogId={}", event.gradingLogId(), e);
            throw e;
        }
    }
}
