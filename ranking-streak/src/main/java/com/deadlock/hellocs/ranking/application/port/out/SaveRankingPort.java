package com.deadlock.hellocs.ranking.application.port.out;

import com.deadlock.hellocs.ranking.domain.RankingKey;

/**
 * 랭킹 점수를 기록/갱신하는 아웃바운드 포트.
 *
 * <p>특정 보드({@link RankingKey})에 대한 사용자의 점수를 "가산"하는 것이 유일한 책임이다.
 * 덮어쓰기가 아닌 incrementScore 방식이므로 여러 이벤트가 누적되어도 일관되게 반영된다.</p>
 */
public interface SaveRankingPort {

    void incrementScore(RankingKey key, Long userId, int score);
}
