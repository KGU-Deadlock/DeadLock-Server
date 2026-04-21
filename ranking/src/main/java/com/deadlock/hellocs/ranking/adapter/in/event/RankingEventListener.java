package com.deadlock.hellocs.ranking.adapter.in.event;

import com.deadlock.hellocs.ranking.application.port.in.UpdateRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.dto.UpdateRankingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingEventListener {

    private final UpdateRankingUseCase updateRankingUseCase;

    @EventListener
    public void handle(QuizCompletedEvent event) {
        updateRankingUseCase.update(new UpdateRankingCommand(
                event.userId(),
                event.topicIds(),
                event.score()
        ));
    }
}
