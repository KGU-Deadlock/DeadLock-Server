package com.deadlock.hellocs.streak.application.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

/**
 * 스트릭 기록 요청 커맨드.
 *
 * <p>한 번의 채점 완료에 대해 사용자 스트릭과 일일 기록에 동시에 반영됨.</p>
 *
 * @param gradingLogId 채점 로그 ID (중복 반영 방지를 위한 멱등성 키)
 * @param userId       스트릭을 갱신할 사용자 ID
 * @param solvedDate   퀴즈를 푼 날짜
 * @param quizCount    이번 세션의 퀴즈 풀이 수
 * @param topicIds     관련 주제 ID 목록
 */
public record RecordStreakCommand(
        @NotBlank String gradingLogId,
        @NotNull Long userId,
        @NotNull LocalDate solvedDate,
        @Positive int quizCount,
        @NotNull List<Long> topicIds
) {}
