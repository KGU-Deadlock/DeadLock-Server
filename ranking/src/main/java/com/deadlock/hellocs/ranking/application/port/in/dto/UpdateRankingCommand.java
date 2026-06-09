package com.deadlock.hellocs.ranking.application.port.in.dto;

import java.util.List;

/**
 * 랭킹 점수 갱신 요청 커맨드.
 *
 * <p>하나의 퀴즈 세션에 대한 점수가 전체 랭킹과
 * 해당 퀴즈의 모든 주제별 랭킹에 동시에 누적된다.</p>
 *
 * @param userId   점수를 적립할 사용자 ID
 * @param topicIds 점수에 함께 반영할 주제 ID 목록 (여러 주제에 동시 반영)
 * @param score    이번 세션에서 획득한 점수 (기존 점수에 가산된다)
 */
public record UpdateRankingCommand(
        Long userId,
        List<Long> topicIds,
        int score
) {}
