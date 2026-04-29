package com.deadlock.hellocs.streak.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.streak.adapter.in.web.docs.StreakControllerDocs;
import com.deadlock.hellocs.streak.application.port.in.QueryStreakUseCase;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public ApiResponse<StreakSummaryResult> getSummary(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(queryStreakUseCase.getSummary(userId));
    }

    @GetMapping(params = {"year", "month"})
    @Override
    public ApiResponse<StreakMonthlyResult> getMonthly(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @Min(2000) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(queryStreakUseCase.getMonthly(userId, year, month));
    }

    @GetMapping("/detail")
    @Override
    public ApiResponse<StreakDetailResult> getDetail(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(queryStreakUseCase.getDetail(userId));
    }
}
