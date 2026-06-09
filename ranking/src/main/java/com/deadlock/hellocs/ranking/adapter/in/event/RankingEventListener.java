package com.deadlock.hellocs.ranking.adapter.in.event;

import com.deadlock.hellocs.ranking.application.port.in.UpdateRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.dto.UpdateRankingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 채점 완료 메시지를 수신하여 랭킹 점수를 갱신하는 인바운드 어댑터.
 */
@Component
@RequiredArgsConstructor
public class RankingEventListener {

    private final UpdateRankingUseCase updateRankingUseCase;

    @RabbitListener(queues = "ranking.grading.completed")
    public void handle(GradingCompletedMessage message) {
        updateRankingUseCase.update(new UpdateRankingCommand(
                message.userId(),
                message.topicIds(),
                message.totalScore()
        ));
    }
}
