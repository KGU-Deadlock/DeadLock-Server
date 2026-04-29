package com.deadlock.hellocs.ranking.adapter.in.event;

import com.deadlock.hellocs.ranking.application.port.in.UpdateRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.dto.UpdateRankingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 퀴즈 완료 이벤트를 수신하여 랭킹 점수를 갱신하는 인바운드 어댑터.
 *
 * <p>이벤트 드리븐 구조로, 퀴즈 모듈과 랭킹 모듈의 직접 의존을 끊어준다.
 * 현재는 동기식 {@link EventListener}로 동작하며, 필요 시 {@code @Async}나
 * 별도 메시지 브로커로 이관할 수 있다.</p>
 */
@Component
@RequiredArgsConstructor
public class RankingEventListener {

    private final UpdateRankingUseCase updateRankingUseCase;

    /**
     * 수신한 이벤트를 {@link UpdateRankingCommand}로 변환하여 유스케이스에 위임함.
     * 어댑터 계층에서는 변환만 담당하고 비즈니스 로직은 서비스가 책임진다.
     */
    @EventListener
    public void handle(QuizCompletedEvent event) {
        updateRankingUseCase.update(new UpdateRankingCommand(
                event.userId(),
                event.topicIds(),
                event.score()
        ));
    }
}
