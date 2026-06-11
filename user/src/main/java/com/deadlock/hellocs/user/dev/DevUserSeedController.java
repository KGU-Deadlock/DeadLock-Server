package com.deadlock.hellocs.user.dev;

import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import com.deadlock.hellocs.user.application.port.in.CreateUserUseCase;
import com.deadlock.hellocs.user.application.port.in.LoadUserUseCase;
import com.deadlock.hellocs.user.application.port.in.dto.UserSignUpCommand;
import com.deadlock.hellocs.user.domain.UserLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * dev 서비스가 호출하는 유저 시딩 엔드포인트. dev 프로파일에서만 활성화된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/dev")
public class DevUserSeedController {

    private final CreateUserUseCase createUserUseCase;
    private final LoadUserUseCase loadUserUseCase;

    private final AtomicInteger seedProcessed = new AtomicInteger(0);
    private volatile int seedTotal = 0;

    /**
     * 테스트 유저 생성. kakaoId 1001~(1000+count) 범위.
     * 이미 존재하는 유저는 건너뛴다.
     *
     * @param count     생성할 유저 수
     * @param numTopics 관심 토픽 수 (ID 1~numTopics, 유저별 1개 할당). 기본 6.
     */
    @PostMapping("/users")
    public ApiResponse<SeedUsersResult> seedUsers(
            @RequestParam(name = "count", defaultValue = "10") int count,
            @RequestParam(name = "numTopics", defaultValue = "6") int numTopics) {

        UserLevel[] levels = UserLevel.values();
        List<Long> kakaoIds = new ArrayList<>();
        int created = 0;
        int safeNumTopics = Math.max(1, numTopics);

        seedTotal = count;
        seedProcessed.set(0);

        for (int i = 1; i <= count; i++) {
            long kakaoId = 1000L + i;
            kakaoIds.add(kakaoId);
            seedProcessed.incrementAndGet();

            if (loadUserUseCase.isExist(kakaoId)) continue;

            String nickname = String.format("devuser%05d", i);
            // 관심 토픽: kakaoId 기준 1개 — 토픽 ID 1~numTopics를 순환 배정
            long interestTopicId = (long)((i - 1) % safeNumTopics) + 1L;
            createUserUseCase.createUser(kakaoId, new UserSignUpCommand(
                    nickname,
                    nickname + "@example.com",
                    "https://picsum.photos/seed/" + nickname + "/200/200",
                    levels[(i - 1) % levels.length],
                    List.of(interestTopicId)
            ));
            created++;
        }

        return ApiResponse.onSuccess(new SeedUsersResult(created, kakaoIds));
    }

    /** 유저 시딩 진행 상황 조회. */
    @GetMapping("/users/progress")
    public ApiResponse<SeedUsersProgressResult> getUserSeedProgress() {
        return ApiResponse.onSuccess(new SeedUsersProgressResult(seedProcessed.get(), seedTotal));
    }

    /** 특정 kakaoId 유저의 존재 여부 확인. */
    @GetMapping("/users/{kakaoId}/exists")
    public ApiResponse<Boolean> userExists(@PathVariable Long kakaoId) {
        return ApiResponse.onSuccess(loadUserUseCase.isExist(kakaoId));
    }

    public record SeedUsersResult(int created, List<Long> kakaoIds) {}
    public record SeedUsersProgressResult(int processed, int total) {}
}
