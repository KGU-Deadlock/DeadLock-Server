package com.deadlock.hellocs.dev.service;

import com.deadlock.hellocs.dev.config.DevJwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
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

    public SeedStatsResult seedStats(int users, int signupWindowDays, int quizPerDay, int numTopics,
                                     float segPowerShare, float segRegularShare,
                                     int segPowerDpw, int segRegularDpw, int segCasualDpw,
                                     int tokenPoolSize, long seed) {
        SeedUsersResponse usersResp = userClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/users")
                        .queryParam("count",     users)
                        .queryParam("numTopics", numTopics)
                        .build())
                .retrieve()
                .body(SeedUsersApiResponse.class)
                .data();

        List<UserActivitySpec> specs = buildUserSpecs(
                users, tokenPoolSize, segPowerShare, segRegularShare,
                segPowerDpw, segRegularDpw, segCasualDpw, signupWindowDays);

        SeedGradingLogsResponse gradingResp = gradingClient.post()
                .uri("/v1/internal/dev/grading-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SeedGradingLogsRequest(specs, quizPerDay, numTopics, seed))
                .retrieve()
                .body(SeedGradingLogsApiResponse.class)
                .data();

        SeedStreakResponse streakResp = streakClient.post()
                .uri("/v1/internal/dev/streak-records")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SeedStreakRecordsRequest(specs, quizPerDay, numTopics))
                .retrieve()
                .body(SeedStreakApiResponse.class)
                .data();

        SeedRankingResponse rankingResp = rankingClient.post()
                .uri("/v1/internal/dev/ranking")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SeedRankingRequest(specs, quizPerDay, numTopics, seed))
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
        List<UserActivitySpec> specs = buildUserSpecs(
                users, tokenPoolSize, segPowerShare, segRegularShare,
                segPowerDpw, segRegularDpw, segCasualDpw, signupWindowDays);

        seedActivityProgress.setPhase(SeedActivityProgress.Phase.GRADING);
        SeedGradingLogsResponse gradingResp = gradingClient.post()
                .uri("/v1/internal/dev/grading-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SeedGradingLogsRequest(specs, quizPerDay, numTopics, seed))
                .retrieve()
                .body(SeedGradingLogsApiResponse.class)
                .data();

        seedActivityProgress.setPhase(SeedActivityProgress.Phase.STREAK);
        SeedStreakResponse streakResp = streakClient.post()
                .uri("/v1/internal/dev/streak-records")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SeedStreakRecordsRequest(specs, quizPerDay, numTopics))
                .retrieve()
                .body(SeedStreakApiResponse.class)
                .data();

        seedActivityProgress.setPhase(SeedActivityProgress.Phase.RANKING);
        SeedRankingResponse rankingResp = rankingClient.post()
                .uri("/v1/internal/dev/ranking")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SeedRankingRequest(specs, quizPerDay, numTopics, seed))
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

    // ─── 세그먼트 레이아웃 (단일 정의) ────────────────────────────────────────

    private List<UserActivitySpec> buildUserSpecs(int users, int tokenPoolSize,
            float segPowerShare, float segRegularShare,
            int segPowerDpw, int segRegularDpw, int segCasualDpw,
            int signupWindowDays) {
        SegmentLayout layout = new SegmentLayout(users, tokenPoolSize, segPowerShare, segRegularShare);
        List<UserActivitySpec> specs = new ArrayList<>(users);
        for (int idx = 0; idx < users; idx++) {
            long kakaoId    = 1001L + idx;
            int  dpw        = layout.dpwOf(idx, segPowerDpw, segRegularDpw, segCasualDpw);
            int  accountAge = layout.accountAgeDays(idx, signupWindowDays);
            specs.add(new UserActivitySpec(kakaoId, dpw, accountAge));
        }
        return specs;
    }

    private static final class SegmentLayout {

        private final int users, tokenPoolSize;
        private final int poolPower, poolRegular, poolCasual;
        private final int totalPower, totalRegular;
        private final int bgPower, bgRegular;

        SegmentLayout(int users, int tokenPoolSize, float powerShare, float regularShare) {
            this.users         = users;
            this.tokenPoolSize = Math.min(tokenPoolSize, users);
            this.poolPower     = (int) (this.tokenPoolSize * powerShare);
            this.poolRegular   = (int) (this.tokenPoolSize * regularShare);
            this.poolCasual    = this.tokenPoolSize - this.poolPower - this.poolRegular;
            this.totalPower    = (int) (users * powerShare);
            this.totalRegular  = (int) (users * regularShare);
            this.bgPower       = Math.max(0, totalPower   - poolPower);
            this.bgRegular     = Math.max(0, totalRegular - poolRegular);
        }

        int dpwOf(int idx, int powerDpw, int regularDpw, int casualDpw) {
            return switch (segmentOf(idx)) {
                case POWER   -> powerDpw;
                case REGULAR -> regularDpw;
                case CASUAL  -> casualDpw;
            };
        }

        int accountAgeDays(int idx, int signupWindowDays) {
            int[] segInfo = segmentIdxAndSize(idx);
            int segIdx  = segInfo[0];
            int segSize = segInfo[1];
            if (segSize <= 1) return signupWindowDays;
            return (segSize - 1 - segIdx) * signupWindowDays / (segSize - 1);
        }

        private Segment segmentOf(int idx) {
            if (idx < poolPower)                   return Segment.POWER;
            if (idx < poolPower + poolRegular)     return Segment.REGULAR;
            if (idx < tokenPoolSize)               return Segment.CASUAL;
            int bgIdx = idx - tokenPoolSize;
            if (bgIdx < bgPower)                   return Segment.POWER;
            if (bgIdx < bgPower + bgRegular)       return Segment.REGULAR;
            return Segment.CASUAL;
        }

        private int[] segmentIdxAndSize(int idx) {
            if (idx < poolPower)
                return new int[]{ idx, poolPower };
            if (idx < poolPower + poolRegular)
                return new int[]{ idx - poolPower, poolRegular };
            if (idx < tokenPoolSize)
                return new int[]{ idx - poolPower - poolRegular, poolCasual };
            int bgIdx = idx - tokenPoolSize;
            if (bgIdx < bgPower)
                return new int[]{ bgIdx, bgPower };
            if (bgIdx < bgPower + bgRegular)
                return new int[]{ bgIdx - bgPower, bgRegular };
            int bgCasual = users - tokenPoolSize - bgPower - bgRegular;
            return new int[]{ bgIdx - bgPower - bgRegular, Math.max(1, bgCasual) };
        }

        enum Segment { POWER, REGULAR, CASUAL }
    }

    // ─── 결과 레코드 ───────────────────────────────────────────────────────────

    public record SeedTopicsResult(int created, int alreadyExisted) {}
    public record SeedQuizBankResult(int topicCount, int quizzesCreated) {}
    public record SeedCsQuestionsResult(int categoryCount, int questionsCreated) {}

    public record SeedUsersOnlyResult(int usersCreated) {}

    public record SeedActivityResult(int gradingDocsCreated, int streakDailyRecordsCreated, int rankingMembersCreated) {}

    public record GradingProgressData(int processedUsers, int totalUsers, long insertedDocs) {}

    public record ActivityProgressResult(String phase, long elapsedSeconds, GradingProgressData grading) {}

    public record SeedStatsResult(
            int usersCreated,
            int gradingDocsCreated,
            int streakDailyRecordsCreated,
            int rankingMembersCreated
    ) {}

    public record AdminTokenResult(String accessToken, String refreshToken, boolean isUser) {}
    public record UserTokenResult(String accessToken, String refreshToken, boolean isUser) {}

    // ─── 내부 DTO (요청 바디) ──────────────────────────────────────────────────

    private record UserActivitySpec(long kakaoId, int dpw, int accountAgeDays) {}
    private record SeedGradingLogsRequest(List<UserActivitySpec> users, int quizPerDay, int numTopics, long seed) {}
    private record SeedStreakRecordsRequest(List<UserActivitySpec> users, int quizPerDay, int numTopics) {}
    private record SeedRankingRequest(List<UserActivitySpec> users, int quizPerDay, int numTopics, long seed) {}

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
