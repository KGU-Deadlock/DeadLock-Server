package com.deadlock.hellocs.ranking.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.ranking.adapter.in.web.docs.RankingControllerDocs;
import com.deadlock.hellocs.ranking.application.port.in.QueryRankingUseCase;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingQueryType;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
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

@RestController
@RequestMapping("/v1/ranking")
@RequiredArgsConstructor
@Validated
public class RankingController implements RankingControllerDocs {

    private static final int SUMMARY_SIZE = 5;

    private final QueryRankingUseCase queryRankingUseCase;

    @GetMapping("/summary")
    @Override
    public ApiResponse<RankingSummaryResult> getRankingSummary() {
        return ApiResponse.onSuccess(queryRankingUseCase.getSummary(SUMMARY_SIZE));
    }

    @GetMapping
    @Override
    public ApiResponse<RankingDetailResult> getRanking(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "filterType", defaultValue = "ALL") RankingQueryType type,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(queryRankingUseCase.getRankingByType(userId, type, size));
    }
}
