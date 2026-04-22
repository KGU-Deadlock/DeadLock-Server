package com.deadlock.hellocs.streak.application.port.in;

import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 스트릭 조회(Query) 인바운드 포트.
 *
 * <p>헥사고날 아키텍처의 Inbound Port로, REST 컨트롤러에서 스트릭 데이터를 읽어갈 때 사용하는 유스케이스 계약임.</p>
 */
public interface QueryStreakUseCase {

    /** 현재 스트릭 일수·총 풀이 수·학습 분야 수 요약을 반환함. */
    StreakSummaryResult getSummary(@NotNull Long userId);

    /** 요약 정보에 최장 스트릭, 오늘 풀이 여부, 이번 달 통계를 추가한 상세 정보를 반환함. */
    StreakDetailResult getDetail(@NotNull Long userId);

    /** 지정한 연월의 일자별 스트릭 캘린더(모든 날짜 포함)를 반환함. */
    StreakMonthlyResult getMonthly(
            @NotNull Long userId,
            @Min(2000) int year,
            @Min(1) @Max(12) int month
    );
}
