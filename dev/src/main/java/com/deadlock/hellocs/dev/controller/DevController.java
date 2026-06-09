package com.deadlock.hellocs.dev.controller;

import com.deadlock.hellocs.dev.service.DevSeedService;
import com.deadlock.hellocs.common.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/dev")
public class DevController {

    private final DevSeedService devSeedService;

    @PostMapping("/seed/topics")
    public ApiResponse<DevSeedService.SeedTopicsResult> seedTopics() {
        return ApiResponse.onSuccess(devSeedService.seedTopics());
    }

    @PostMapping("/seed/quiz-bank")
    public ApiResponse<DevSeedService.SeedQuizBankResult> seedQuizBank(
            @RequestParam(name = "perCombo", defaultValue = "5") int perCombo) {
        return ApiResponse.onSuccess(devSeedService.seedQuizBank(perCombo));
    }

    @PostMapping("/seed/cs-questions")
    public ApiResponse<DevSeedService.SeedCsQuestionsResult> seedCsQuestions(
            @RequestParam(name = "perCategory", defaultValue = "10") int perCategory) {
        return ApiResponse.onSuccess(devSeedService.seedCsQuestions(perCategory));
    }

    @PostMapping("/seed/stats")
    public ApiResponse<DevSeedService.SeedStatsResult> seedStats(
            @RequestParam(name = "userCount", defaultValue = "10") int userCount,
            @RequestParam(name = "days", defaultValue = "14") int days,
            @RequestParam(name = "fromIndex", defaultValue = "0") int fromIndex) {
        return ApiResponse.onSuccess(devSeedService.seedStats(userCount, days, fromIndex));
    }

    @GetMapping("/admin-token")
    public ApiResponse<DevSeedService.AdminTokenResult> getAdminToken() {
        return ApiResponse.onSuccess(devSeedService.getAdminToken());
    }

    @GetMapping("/user-token")
    public ApiResponse<DevSeedService.UserTokenResult> getUserToken(
            @RequestParam(name = "kakaoId") Long kakaoId) {
        return ApiResponse.onSuccess(devSeedService.getUserToken(kakaoId));
    }
}
