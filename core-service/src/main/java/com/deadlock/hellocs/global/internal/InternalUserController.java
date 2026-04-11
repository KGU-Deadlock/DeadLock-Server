package com.deadlock.hellocs.global.internal;

import com.deadlock.hellocs.user.application.port.in.LoadUserSummaryUseCase;
import com.deadlock.hellocs.user.application.port.in.UserSummaryResult;
import com.deadlock.hellocs.user.application.port.out.LoadUserPort;
import com.deadlock.hellocs.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ranking-streak-service → core-service 서비스 간 내부 API.
 * 외부 인터넷에 노출되지 않도록 nginx 레벨에서 /internal/** 차단 권고.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/users")
public class InternalUserController {

    private final LoadUserSummaryUseCase loadUserSummaryUseCase;
    private final LoadUserPort loadUserPort;

    @GetMapping("/summary")
    public UserSummaryResult getUserSummary(@RequestParam Long kakaoId) {
        return loadUserSummaryUseCase.getUserSummary(kakaoId);
    }

    @GetMapping("/summaries")
    public List<UserSummaryResult> getUserSummaries(@RequestParam List<Long> kakaoIds) {
        return loadUserSummaryUseCase.getUserSummaries(kakaoIds);
    }

    @GetMapping("/{kakaoId}/detail")
    public UserDetailResponse getUserDetail(@PathVariable Long kakaoId) {
        User user = loadUserPort.loadUserByKakaoId(kakaoId);
        return new UserDetailResponse(user.getKakaoId(), user.getInterestTopicIds());
    }

    @PostMapping("/details")
    public List<UserDetailResponse> getUserDetails(@RequestBody List<Long> kakaoIds) {
        return loadUserPort.loadUsersByKakaoIds(kakaoIds).stream()
                .map(u -> new UserDetailResponse(u.getKakaoId(), u.getInterestTopicIds()))
                .toList();
    }

    public record UserDetailResponse(Long kakaoId, List<Long> interestTopicIds) {}
}
