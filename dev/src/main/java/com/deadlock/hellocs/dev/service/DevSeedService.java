package com.deadlock.hellocs.dev.service;

import com.deadlock.hellocs.dev.config.DevJwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
public class DevSeedService {

    private final RestClient           userClient;
    private final RestClient           topicClient;
    private final RestClient           quizClient;
    private final RestClient           interviewClient;
    private final RestClient           gradingClient;
    private final RestClient           streakClient;
    private final RestClient           rankingClient;
    private final DevJwtTokenProvider  jwtTokenProvider;
    private final SeedActivityProgress seedActivityProgress;

    public DevSeedService(
            @Value("${service.user.url}")      String userUrl,
            @Value("${service.topic.url}")     String topicUrl,
            @Value("${service.quiz.url}")      String quizUrl,
            @Value("${service.interview.url}") String interviewUrl,
            @Value("${service.grading.url}")   String gradingUrl,
            @Value("${service.streak.url}")    String streakUrl,
            @Value("${service.ranking.url}")   String rankingUrl,
            DevJwtTokenProvider jwtTokenProvider,
            SeedActivityProgress seedActivityProgress) {
        this.userClient           = RestClient.create(userUrl);
        this.topicClient          = RestClient.create(topicUrl);
        this.quizClient           = RestClient.create(quizUrl);
        this.interviewClient      = RestClient.create(interviewUrl);
        this.gradingClient        = RestClient.create(gradingUrl);
        this.streakClient         = RestClient.create(streakUrl);
        this.rankingClient        = RestClient.create(rankingUrl);
        this.jwtTokenProvider     = jwtTokenProvider;
        this.seedActivityProgress = seedActivityProgress;
    }

    // ─── 토픽 시딩 ─────────────────────────────────────────────────────────────

    public SeedTopicsResult seedTopics() {
        SeedTopicsResponse resp = topicClient.post()
                .uri("/v1/internal/dev/topics")
                .retrieve()
                .body(SeedTopicsApiResponse.class)
                .data();
        return new SeedTopicsResult(resp.created(), resp.alreadyExisted());
    }

    // ─── 퀴즈 뱅크 시딩 ────────────────────────────────────────────────────────

    public SeedQuizBankResult seedQuizBank(int perCombo) {
        SeedQuizBankResponse resp = quizClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/quiz-bank")
                        .queryParam("perCombo", perCombo)
                        .build())
                .retrieve()
                .body(SeedQuizBankApiResponse.class)
                .data();
        return new SeedQuizBankResult(resp.topicCount(), resp.quizzesCreated());
    }

    // ─── CS 질문 시딩 (interview-service, extra 프로파일) ─────────────────────

    public SeedCsQuestionsResult seedCsQuestions(int perCategory) {
        SeedCsQuestionsResponse resp = interviewClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/cs-questions")
                        .queryParam("perCategory", perCategory)
                        .build())
                .retrieve()
                .body(SeedCsQuestionsApiResponse.class)
                .data();
        return new SeedCsQuestionsResult(resp.categoryCount(), resp.questionsCreated());
    }

    // ─── 통계 시딩 (유저 + 채점 기록 + 스트릭 + 랭킹 위임) ──────────────────

    /**
     * 세그먼트 분포 모델 기반 통계 시딩.
     * dataset.env 파라미터를 그대로 전달하며 다음 순서로 시딩한다:
     * 1) user-service  : 유저 생성 (관심 토픽 포함)
     * 2) grading-service: grading_logs bulk insert (이벤트 없음)
     * 3) streak-service : streak records bulk write
     * 4) ranking-service: ranking ZADD
     */
    public SeedStatsResult seedStats(int users, int signupWindowDays, int quizPerDay, int numTopics,
                                     float segPowerShare, float segRegularShare,
                                     int segPowerDpw, int segRegularDpw, int segCasualDpw,
                                     int tokenPoolSize, long seed) {
        // 1. 유저 생성
        SeedUsersResponse usersResp = userClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/users")
                        .queryParam("count",     users)
                        .queryParam("numTopics", numTopics)
                        .build())
                .retrieve()
                .body(SeedUsersApiResponse.class)
                .data();

        // 2. 채점 기록 시딩 (이벤트 없음 — 파생 데이터는 3,4에서 직접 생성)
        SeedGradingLogsResponse gradingResp = gradingClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/grading-logs")
                        .queryParam("users",            users)
                        .queryParam("signupWindowDays", signupWindowDays)
                        .queryParam("quizPerDay",       quizPerDay)
                        .queryParam("numTopics",        numTopics)
                        .queryParam("segPowerShare",    segPowerShare)
                        .queryParam("segRegularShare",  segRegularShare)
                        .queryParam("segPowerDpw",      segPowerDpw)
                        .queryParam("segRegularDpw",    segRegularDpw)
                        .queryParam("segCasualDpw",     segCasualDpw)
                        .queryParam("tokenPoolSize",    tokenPoolSize)
                        .queryParam("seed",             seed)
                        .build())
                .retrieve()
                .body(SeedGradingLogsApiResponse.class)
                .data();

        // 3. 스트릭 기록 시딩
        SeedStreakResponse streakResp = streakClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/streak-records")
                        .queryParam("users",            users)
                        .queryParam("signupWindowDays", signupWindowDays)
                        .queryParam("quizPerDay",       quizPerDay)
                        .queryParam("numTopics",        numTopics)
                        .queryParam("segPowerShare",    segPowerShare)
                        .queryParam("segRegularShare",  segRegularShare)
                        .queryParam("segPowerDpw",      segPowerDpw)
                        .queryParam("segRegularDpw",    segRegularDpw)
                        .queryParam("segCasualDpw",     segCasualDpw)
                        .queryParam("tokenPoolSize",    tokenPoolSize)
                        .build())
                .retrieve()
                .body(SeedStreakApiResponse.class)
                .data();

        // 4. 랭킹 시딩
        SeedRankingResponse rankingResp = rankingClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/ranking")
                        .queryParam("users",            users)
                        .queryParam("signupWindowDays", signupWindowDays)
                        .queryParam("quizPerDay",       quizPerDay)
                        .queryParam("numTopics",        numTopics)
                        .queryParam("segPowerShare",    segPowerShare)
                        .queryParam("segRegularShare",  segRegularShare)
                        .queryParam("segPowerDpw",      segPowerDpw)
                        .queryParam("segRegularDpw",    segRegularDpw)
                        .queryParam("segCasualDpw",     segCasualDpw)
                        .queryParam("tokenPoolSize",    tokenPoolSize)
                        .queryParam("seed",             seed)
                        .build())
                .retrieve()
                .body(SeedRankingApiResponse.class)
                .data();

        return new SeedStatsResult(
                usersResp.created(),
                gradingResp.docsCreated(),
                streakResp.dailyRecordsCreated(),
                rankingResp.totalMembers()
        );
    }

    // ─── 유저 시드 (users 전용) ────────────────────────────────────────────────

    public SeedUsersOnlyResult seedUsersOnly(int users, int numTopics) {
        SeedUsersResponse resp = userClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/users")
                        .queryParam("count",     users)
                        .queryParam("numTopics", numTopics)
                        .build())
                .retrieve()
                .body(SeedUsersApiResponse.class)
                .data();
        return new SeedUsersOnlyResult(resp.created());
    }

    // ─── 채점·스트릭·랭킹 시드 (activity) ─────────────────────────────────────

    public SeedActivityResult seedActivity(int users, int signupWindowDays, int quizPerDay, int numTopics,
                                           float segPowerShare, float segRegularShare,
                                           int segPowerDpw, int segRegularDpw, int segCasualDpw,
                                           int tokenPoolSize, long seed) {
        seedActivityProgress.start();
        try {
            return doSeedActivity(users, signupWindowDays, quizPerDay, numTopics,
                    segPowerShare, segRegularShare,
                    segPowerDpw, segRegularDpw, segCasualDpw,
                    tokenPoolSize, seed);
        } catch (Exception e) {
            seedActivityProgress.error(e.getMessage());
            throw e;
        }
    }

    private SeedActivityResult doSeedActivity(int users, int signupWindowDays, int quizPerDay, int numTopics,
                                              float segPowerShare, float segRegularShare,
                                              int segPowerDpw, int segRegularDpw, int segCasualDpw,
                                              int tokenPoolSize, long seed) {
        seedActivityProgress.setPhase(SeedActivityProgress.Phase.GRADING);
        SeedGradingLogsResponse gradingResp = gradingClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/grading-logs")
                        .queryParam("users",            users)
                        .queryParam("signupWindowDays", signupWindowDays)
                        .queryParam("quizPerDay",       quizPerDay)
                        .queryParam("numTopics",        numTopics)
                        .queryParam("segPowerShare",    segPowerShare)
                        .queryParam("segRegularShare",  segRegularShare)
                        .queryParam("segPowerDpw",      segPowerDpw)
                        .queryParam("segRegularDpw",    segRegularDpw)
                        .queryParam("segCasualDpw",     segCasualDpw)
                        .queryParam("tokenPoolSize",    tokenPoolSize)
                        .queryParam("seed",             seed)
                        .build())
                .retrieve()
                .body(SeedGradingLogsApiResponse.class)
                .data();

        seedActivityProgress.setPhase(SeedActivityProgress.Phase.STREAK);
        SeedStreakResponse streakResp = streakClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/streak-records")
                        .queryParam("users",            users)
                        .queryParam("signupWindowDays", signupWindowDays)
                        .queryParam("quizPerDay",       quizPerDay)
                        .queryParam("numTopics",        numTopics)
                        .queryParam("segPowerShare",    segPowerShare)
                        .queryParam("segRegularShare",  segRegularShare)
                        .queryParam("segPowerDpw",      segPowerDpw)
                        .queryParam("segRegularDpw",    segRegularDpw)
                        .queryParam("segCasualDpw",     segCasualDpw)
                        .queryParam("tokenPoolSize",    tokenPoolSize)
                        .build())
                .retrieve()
                .body(SeedStreakApiResponse.class)
                .data();

        seedActivityProgress.setPhase(SeedActivityProgress.Phase.RANKING);
        SeedRankingResponse rankingResp = rankingClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/ranking")
                        .queryParam("users",            users)
                        .queryParam("signupWindowDays", signupWindowDays)
                        .queryParam("quizPerDay",       quizPerDay)
                        .queryParam("numTopics",        numTopics)
                        .queryParam("segPowerShare",    segPowerShare)
                        .queryParam("segRegularShare",  segRegularShare)
                        .queryParam("segPowerDpw",      segPowerDpw)
                        .queryParam("segRegularDpw",    segRegularDpw)
                        .queryParam("segCasualDpw",     segCasualDpw)
                        .queryParam("tokenPoolSize",    tokenPoolSize)
                        .queryParam("seed",             seed)
                        .build())
                .retrieve()
                .body(SeedRankingApiResponse.class)
                .data();

        seedActivityProgress.setPhase(SeedActivityProgress.Phase.DONE);
        return new SeedActivityResult(
                gradingResp.docsCreated(),
                streakResp.dailyRecordsCreated(),
                rankingResp.totalMembers()
        );
    }

    // ─── activity 진행상황 조회 ────────────────────────────────────────────────

    public ActivityProgressResult getActivityProgress() {
        SeedActivityProgress.Snapshot snap = seedActivityProgress.snapshot();
        GradingProgressData gradingData = null;
        if (snap.phase() == SeedActivityProgress.Phase.GRADING) {
            try {
                GradingProgressApiResponse resp = gradingClient.get()
                        .uri("/v1/internal/dev/grading-logs/progress")
                        .retrieve()
                        .body(GradingProgressApiResponse.class);
                if (resp != null && resp.data() != null) {
                    GradingSnapshotData d = resp.data();
                    gradingData = new GradingProgressData(d.processedUsers(), d.totalUsers(), d.insertedDocs());
                }
            } catch (Exception ignored) {}
        }
        return new ActivityProgressResult(snap.phase().name(), snap.elapsedSeconds(), gradingData);
    }

    // ─── 토큰 발급 ─────────────────────────────────────────────────────────────

    public AdminTokenResult getAdminToken() {
        Long kakaoId = 1L;
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(kakaoId), "ADMIN");
        String refreshToken = jwtTokenProvider.createRefreshToken(String.valueOf(kakaoId));
        boolean isUser = checkUserExists(kakaoId);
        return new AdminTokenResult(accessToken, refreshToken, isUser);
    }

    public UserTokenResult getUserToken(Long kakaoId) {
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(kakaoId), "USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(String.valueOf(kakaoId));
        boolean isUser = checkUserExists(kakaoId);
        return new UserTokenResult(accessToken, refreshToken, isUser);
    }

    private boolean checkUserExists(Long kakaoId) {
        try {
            Boolean result = userClient.get()
                    .uri("/v1/internal/dev/users/{kakaoId}/exists", kakaoId)
                    .retrieve()
                    .body(UserExistsApiResponse.class)
                    .data();
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("유저 존재 여부 확인 실패 (kakaoId={}): {}", kakaoId, e.getMessage());
            return false;
        }
    }

    // ─── 결과 레코드 ───────────────────────────────────────────────────────────

    public record SeedTopicsResult(int created, int alreadyExisted) {}
    public record SeedQuizBankResult(int topicCount, int quizzesCreated) {}
    public record SeedCsQuestionsResult(int categoryCount, int questionsCreated) {}

    public record SeedUsersOnlyResult(int usersCreated) {}

    public record SeedActivityResult(int gradingDocsCreated, int streakDailyRecordsCreated, int rankingMembersCreated) {}

    public record GradingProgressData(int processedUsers, int totalUsers, long insertedDocs) {}

    public record ActivityProgressResult(String phase, long elapsedSeconds, GradingProgressData grading) {}

    /** 통계 시딩 결과 (4-서비스 통합). */
    public record SeedStatsResult(
            int usersCreated,
            int gradingDocsCreated,
            int streakDailyRecordsCreated,
            int rankingMembersCreated
    ) {}

    public record AdminTokenResult(String accessToken, String refreshToken, boolean isUser) {}
    public record UserTokenResult(String accessToken, String refreshToken, boolean isUser) {}

    // ─── 내부 응답 역직렬화용 레코드 ──────────────────────────────────────────

    private record SeedTopicsResponse(int created, int alreadyExisted) {}
    private record SeedTopicsApiResponse(boolean isSuccess, SeedTopicsResponse data) {}

    private record SeedQuizBankResponse(int topicCount, int quizzesCreated) {}
    private record SeedQuizBankApiResponse(boolean isSuccess, SeedQuizBankResponse data) {}

    private record SeedCsQuestionsResponse(int categoryCount, int questionsCreated) {}
    private record SeedCsQuestionsApiResponse(boolean isSuccess, SeedCsQuestionsResponse data) {}

    private record SeedUsersResponse(int created, List<Long> kakaoIds) {}
    private record SeedUsersApiResponse(boolean isSuccess, SeedUsersResponse data) {}

    private record SeedGradingLogsResponse(int docsCreated, int itemsCreated) {}
    private record SeedGradingLogsApiResponse(boolean isSuccess, SeedGradingLogsResponse data) {}

    private record SeedStreakResponse(int dailyRecordsCreated, int userStreaksCreated) {}
    private record SeedStreakApiResponse(boolean isSuccess, SeedStreakResponse data) {}

    private record SeedRankingResponse(int totalMembers, int topicKeysCreated) {}
    private record SeedRankingApiResponse(boolean isSuccess, SeedRankingResponse data) {}

    private record UserExistsApiResponse(boolean isSuccess, Boolean data) {}

    private record GradingSnapshotData(boolean running, int processedUsers, int totalUsers, long insertedDocs) {}
    private record GradingProgressApiResponse(boolean isSuccess, GradingSnapshotData data) {}
}
