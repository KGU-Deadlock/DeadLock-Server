package com.deadlock.hellocs.ranking.adapter.in.event;

import java.util.List;

/**
 * 퀴즈 채점이 완료되었음을 알리는 이벤트.
 *
 * <p>quiz 모듈에서 채점 완료 시 발행되며, 랭킹 모듈은 {@code RankingEventListener}에서
 * 이 이벤트를 수신하여 점수를 누적함.
 * 모듈 간 결합을 낮추기 위해 랭킹 모듈 내부에 동일한 형태의 이벤트 레코드를 두고 수신함.</p>
 *
 * @param userId   점수를 적립할 사용자 ID
 * @param topicIds 이번 퀴즈가 속한 주제 ID 목록 (모든 주제 랭킹에 동시 반영)
 * @param score    획득한 점수
 */
public record QuizCompletedEvent(
        Long userId,
        List<Long> topicIds,
        int score
) {}
