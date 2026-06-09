package com.deadlock.hellocs.streak.adapter.in.web;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.common.web.resolver.CurrentUser;
import com.deadlock.hellocs.streak.adapter.in.web.docs.StreakControllerDocs;
import com.deadlock.hellocs.streak.application.port.in.QueryStreakUseCase;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/streak")
public class StreakController implements StreakControllerDocs {

    private final QueryStreakUseCase queryStreakUseCase;

    @GetMapping
    @Override
    public ApiResponse<StreakSummaryResult> getSummary(@CurrentUser Long userId) {
        return ApiResponse.onSuccess(queryStreakUseCase.getSummary(userId));
    }

    @GetMapping(params = {"year", "month"})
    @Override
    public ApiResponse<StreakMonthlyResult> getMonthly(
            @CurrentUser Long userId,
            @RequestParam("year") @Min(2000) int year,
            @RequestParam("month") @Min(1) @Max(12) int month
    ) {
        return ApiResponse.onSuccess(queryStreakUseCase.getMonthly(userId, year, month));
    }

    @GetMapping("/detail")
    @Override
    public ApiResponse<StreakDetailResult> getDetail(@CurrentUser Long userId) {
        return ApiResponse.onSuccess(queryStreakUseCase.getDetail(userId));
    }
}
