package com.deadlock.hellocs.streak.application.port.in;

import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public interface QueryStreakInputPort {
    // 수정 표시
    StreakSummaryResult getSummary(@NotNull Long userId);

    StreakDetailResult getDetail(@NotNull Long userId);

    StreakMonthlyResult getMonthly(
            @NotNull Long userId,
            @Min(2000) int year,
            @Min(1) @Max(12) int month
    );}
