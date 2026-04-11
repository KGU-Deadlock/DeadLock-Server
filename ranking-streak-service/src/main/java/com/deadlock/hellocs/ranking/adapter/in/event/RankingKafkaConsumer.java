package com.deadlock.hellocs.ranking.adapter.in.event;

import com.deadlock.hellocs.ranking.application.port.in.ApplyRankingScoreInputPort;
import com.deadlock.hellocs.ranking.application.port.in.dto.ApplyRankingScoreCommand;
import com.deadlock.hellocs.shared.events.GradingCompletedEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingKafkaConsumer {

    private final ApplyRankingScoreInputPort applyRankingScoreInputPort;

    @KafkaListener(
            topics = "grading.completed",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(GradingCompletedEventMessage event) {
        try {
            if (event == null || event.gradingLogId() == null || event.userId() == null) {
                return;
            }
            applyRankingScoreInputPort.apply(new ApplyRankingScoreCommand(
                    event.gradingLogId(),
                    event.userId(),
                    event.totalScore(),
                    event.topicIds() != null ? event.topicIds() : java.util.List.of()
            ));
        } catch (RuntimeException e) {
            log.error("Failed to update ranking. gradingLogId={}", event.gradingLogId(), e);
            throw e; // 재시도를 위해 예외 재던짐
        }
    }
}
