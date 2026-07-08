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
            @RequestParam(name = "unitCount", defaultValue = "20") int unitCount) {
        return ApiResponse.onSuccess(devSeedService.seedQuizBank(unitCount));
    }

    @PostMapping("/seed/cs-questions")
    public ApiResponse<DevSeedService.SeedCsQuestionsResult> seedCsQuestions(
            @RequestParam(name = "perCategory", defaultValue = "10") int perCategory) {
        return ApiResponse.onSuccess(devSeedService.seedCsQuestions(perCategory));
    }

    /**
     * 세그먼트 분포 모델 기반 통계 시딩.
     * 파라미터 기본값은 ops/perf/profiles/dataset.env와 일치한다.
     * bake-dataset.ps1이 dataset.env를 읽어 이 값들을 전달한다.
     */
    @PostMapping("/seed/users")
    public ApiResponse<DevSeedService.SeedUsersOnlyResult> seedUsers(
            @RequestParam(name = "users",     defaultValue = "100") int users,
            @RequestParam(name = "numTopics", defaultValue = "6")   int numTopics) {
        return ApiResponse.onSuccess(devSeedService.seedUsersOnly(users, numTopics));
    }

    @PostMapping("/seed/activity")
    public ApiResponse<DevSeedService.SeedActivityResult> seedActivity(
            @RequestParam(name = "users",            defaultValue = "100")  int   users,
            @RequestParam(name = "signupWindowDays", defaultValue = "180")  int   signupWindowDays,
            @RequestParam(name = "quizPerDay",       defaultValue = "30")   int   quizPerDay,
            @RequestParam(name = "numTopics",        defaultValue = "6")    int   numTopics,
            @RequestParam(name = "segPowerShare",    defaultValue = "0.2")  float segPowerShare,
            @RequestParam(name = "segRegularShare",  defaultValue = "0.5")  float segRegularShare,
            @RequestParam(name = "segPowerDpw",      defaultValue = "7")    int   segPowerDpw,
            @RequestParam(name = "segRegularDpw",    defaultValue = "4")    int   segRegularDpw,
            @RequestParam(name = "segCasualDpw",     defaultValue = "2")    int   segCasualDpw,
            @RequestParam(name = "tokenPoolSize",    defaultValue = "1000") int   tokenPoolSize,
            @RequestParam(name = "seed",             defaultValue = "42")   long  seed) {
        return ApiResponse.onSuccess(devSeedService.seedActivity(
                users, signupWindowDays, quizPerDay, numTopics,
                segPowerShare, segRegularShare,
                segPowerDpw, segRegularDpw, segCasualDpw,
                tokenPoolSize, seed));
    }

    @PostMapping("/seed/stats")
    public ApiResponse<DevSeedService.SeedStatsResult> seedStats(
            @RequestParam(name = "users",            defaultValue = "100")  int   users,
            @RequestParam(name = "signupWindowDays", defaultValue = "180")  int   signupWindowDays,
            @RequestParam(name = "quizPerDay",       defaultValue = "30")   int   quizPerDay,
            @RequestParam(name = "numTopics",        defaultValue = "6")    int   numTopics,
            @RequestParam(name = "segPowerShare",    defaultValue = "0.2")  float segPowerShare,
            @RequestParam(name = "segRegularShare",  defaultValue = "0.5")  float segRegularShare,
            @RequestParam(name = "segPowerDpw",      defaultValue = "7")    int   segPowerDpw,
            @RequestParam(name = "segRegularDpw",    defaultValue = "4")    int   segRegularDpw,
            @RequestParam(name = "segCasualDpw",     defaultValue = "2")    int   segCasualDpw,
            @RequestParam(name = "tokenPoolSize",    defaultValue = "1000") int   tokenPoolSize,
            @RequestParam(name = "seed",             defaultValue = "42")   long  seed) {
        return ApiResponse.onSuccess(devSeedService.seedStats(
                users, signupWindowDays, quizPerDay, numTopics,
                segPowerShare, segRegularShare,
                segPowerDpw, segRegularDpw, segCasualDpw,
                tokenPoolSize, seed));
    }

    @GetMapping("/seed/users/progress")
    public ApiResponse<DevSeedService.UserSeedProgressResult> getUserSeedProgress() {
        return ApiResponse.onSuccess(devSeedService.getUserSeedProgress());
    }

    @GetMapping("/seed/activity/progress")
    public ApiResponse<DevSeedService.ActivityProgressResult> getActivityProgress() {
        return ApiResponse.onSuccess(devSeedService.getActivityProgress());
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
