package com.deadlock.hellocs.streak.application.port.in;

import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;

public interface QueryStreakInputPort {
    // 수정 표시
    StreakSummaryResult getSummary(Long userId);
    // 수정 표시
    StreakDetailResult getDetail(Long userId);
    // 수정 표시
    StreakMonthlyResult getMonthly(Long userId, int year, int month);
}
