package com.deadlock.hellocs.ranking.adapter.in.web;

import com.deadlock.hellocs.global.apiPayload.ApiResponse;
import com.deadlock.hellocs.ranking.application.port.in.QueryRankingInputPort;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingDetailResult;
import com.deadlock.hellocs.ranking.application.port.in.dto.RankingSummaryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

// 수정 표시
@Tag(
        name = "Ranking",
        description = "실시간 랭킹 API"
)
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ranking")
public class RankingController {

    private final QueryRankingInputPort queryRankingInputPort;

    // 수정 표시
    @Operation(
            summary = "실시간 랭킹 요약 조회",
            description = "인증 없이 전역 랭킹 상위 5명을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실시간 랭킹 요약 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/summary")
    public ApiResponse<RankingSummaryResult> getRankingSummary() {
        return ApiResponse.onSuccess(queryRankingInputPort.getSummary());
    }

    // 수정 표시
    @Operation(
            summary = "실시간 랭킹 상세 조회",
            description = "필터 유형에 따라 랭킹 목록, 내 랭킹 정보, 내 아래 2개 순위 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실시간 랭킹 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "filterType 또는 size 파라미터가 올바르지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ApiResponse<RankingDetailResult> getRanking(
            @AuthenticationPrincipal Jwt jwt,
            // 수정 표시
            @Parameter(
                    description = "랭킹 필터 유형입니다. ALL 또는 INTEREST를 사용합니다.",
                    schema = @Schema(defaultValue = "ALL", allowableValues = {"ALL", "INTEREST"})
            )
            @RequestParam(defaultValue = "ALL") String filterType,
            // 수정 표시
            @Parameter(
                    description = "조회할 랭킹 인원 수입니다. 기본값은 10이며 최대 100명까지 조회할 수 있습니다.",
                    schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100")
            )
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Long kakaoId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(queryRankingInputPort.getRanking(kakaoId, filterType, size));
    }
}
