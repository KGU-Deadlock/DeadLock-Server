package com.deadlock.hellocs.streak.adapter.in.web;

import com.deadlock.hellocs.streak.application.port.in.QueryStreakUseCase;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakDetailResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakMonthlyResult;
import com.deadlock.hellocs.streak.application.port.in.dto.StreakSummaryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 스트릭 조회 REST 컨트롤러.
 *
 * <ul>
 *     <li>{@code GET /api/v1/streak}              — 현재 스트릭 요약 (스트릭 일수, 풀이 수, 분야 수)</li>
 *     <li>{@code GET /api/v1/streak?year=&month=} — 월별 일자별 스트릭 캘린더</li>
 *     <li>{@code GET /api/v1/streak/detail}       — 상세 통계 (최장 스트릭, 오늘 풀이 여부 등)</li>
 * </ul>
 */
@Tag(
        name = "Streak",
        description = "연속 학습 스트릭 조회 API입니다. 하루 이상 퀴즈를 풀지 않으면 현재 스트릭은 초기화됩니다."
)
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/streak")
public class StreakController {

    private final QueryStreakUseCase queryStreakUseCase;

    @Operation(
            summary = "연속 스트릭 요약 조회",
            description = "현재 연속 학습일, 해결한 문제 수, 해결한 분야 수를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스트릭 요약 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요함"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<StreakSummaryResult> getSummary(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(queryStreakUseCase.getSummary(userId));
    }

    @Operation(
            summary = "연속 스트릭 기간 조회",
            description = "지정한 연도와 월의 일자별 스트릭 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "월별 스트릭 기록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "연도 또는 월 파라미터가 올바르지 않음"),
            @ApiResponse(responseCode = "401", description = "인증이 필요함"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping(params = {"year", "month"})
    public ResponseEntity<StreakMonthlyResult> getMonthly(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    description = "조회할 연도입니다.",
                    schema = @Schema(example = "2025", minimum = "2000")
            )
            @RequestParam @Min(2000) int year,
            @Parameter(
                    description = "조회할 월입니다. 1부터 12 사이의 값을 입력합니다.",
                    schema = @Schema(example = "12", minimum = "1", maximum = "12")
            )
            @RequestParam @Min(1) @Max(12) int month
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(queryStreakUseCase.getMonthly(userId, year, month));
    }

    @Operation(
            summary = "연속 스트릭 상세 조회",
            description = "연속 학습일, 해결한 문제 수, 해결한 분야 수와 함께 상세 통계를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스트릭 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증이 필요함"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/detail")
    public ResponseEntity<StreakDetailResult> getDetail(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(queryStreakUseCase.getDetail(userId));
    }
}
