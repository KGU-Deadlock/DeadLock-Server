package com.deadlock.hellocs.streak.adapter.in.event;

import java.time.LocalDate;
import java.util.List;

/**
 * 퀴즈 채점 완료 시 발행되는 스트릭 갱신 이벤트.
 *
 * <p>quiz 모듈에서 채점이 완료되면 이 이벤트가 발행되고,
 * {@link StreakEventListener}가 수신하여 스트릭 기록을 갱신함.</p>
 *
 * @param gradingLogId 채점 로그 ID (중복 처리 방지용 멱등성 키)
 * @param userId       스트릭을 갱신할 사용자 ID
 * @param solvedDate   퀴즈를 풀이한 날짜
 * @param quizCount    이번 세션에서 풀이한 퀴즈 수
 * @param topicIds     관련 주제 ID 목록
 */
public record StreakUpdateEvent(
        String gradingLogId,
        Long userId,
        LocalDate solvedDate,
        int quizCount,
        List<Long> topicIds
) {}
