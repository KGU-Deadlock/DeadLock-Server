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

@Tag(
        name = "Ranking",
        description = "사용자 점수 기반 랭킹 조회 API"
)
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/ranking")
public class RankingController {

    private final QueryRankingInputPort queryRankingInputPort;

    @Operation(
            summary = "랭킹 요약 조회",
            description = "인증 없이 전체 랭킹 상위 5명의 순위, 사용자 정보, 누적 점수를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "랭킹 요약 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/summary")
    public ApiResponse<RankingSummaryResult> getRankingSummary() {
        return ApiResponse.onSuccess(queryRankingInputPort.getSummary());
    }

    @Operation(
            summary = "랭킹 상세 조회",
            description = "로그인한 사용자를 기준으로 랭킹 목록을 조회합니다. "
                    + "`filterType=ALL`이면 전체 랭킹, `filterType=INTEREST`이면 내 관심 주제 기준 랭킹을 반환하며, "
                    + "응답에는 요청한 랭킹 목록과 내 순위 정보, 내 바로 아래 최대 2명의 순위 정보가 포함됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "랭킹 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "filterType 또는 size 파라미터가 올바르지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ApiResponse<RankingDetailResult> getRanking(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    description = "랭킹 조회 기준입니다. `ALL`은 전체 사용자 기준, `INTEREST`는 로그인한 사용자의 관심 주제 기준 랭킹을 의미합니다.",
                    schema = @Schema(defaultValue = "ALL", allowableValues = {"ALL", "INTEREST"})
            )
            @RequestParam(defaultValue = "ALL") String filterType,
            @Parameter(
                    description = "반환할 랭킹 목록의 개수입니다. 기본값은 10이며 최대 100까지 요청할 수 있습니다.",
                    schema = @Schema(defaultValue = "10", minimum = "1", maximum = "100")
            )
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Long kakaoId = Long.valueOf(jwt.getSubject());
        return ApiResponse.onSuccess(queryRankingInputPort.getRanking(kakaoId, filterType, size));
    }
}
