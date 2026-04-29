package com.deadlock.hellocs.ranking.application.port.in;

import com.deadlock.hellocs.ranking.application.port.in.dto.UpdateRankingCommand;

/**
 * 랭킹 점수 갱신(Command) 인바운드 포트.
 *
 * <p>퀴즈 채점 결과와 같은 외부 이벤트에 의해 점수가 누적될 때 호출된다.
 * 현재는 {@code RankingEventListener}가 {@code QuizCompletedEvent}를 수신하여 이 포트를 호출함.</p>
 */
public interface UpdateRankingUseCase {

    void update(UpdateRankingCommand command);
}
