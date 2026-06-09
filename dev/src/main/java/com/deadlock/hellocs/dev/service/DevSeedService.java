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

    private final RestClient userClient;
    private final RestClient topicClient;
    private final RestClient quizClient;
    private final RestClient interviewClient;
    private final RestClient gradingClient;
    private final DevJwtTokenProvider jwtTokenProvider;

    public DevSeedService(
            @Value("${service.user.url}") String userUrl,
            @Value("${service.topic.url}") String topicUrl,
            @Value("${service.quiz.url}") String quizUrl,
            @Value("${service.interview.url}") String interviewUrl,
            @Value("${service.grading.url}") String gradingUrl,
            DevJwtTokenProvider jwtTokenProvider) {
        this.userClient = RestClient.create(userUrl);
        this.topicClient = RestClient.create(topicUrl);
        this.quizClient = RestClient.create(quizUrl);
        this.interviewClient = RestClient.create(interviewUrl);
        this.gradingClient = RestClient.create(gradingUrl);
        this.jwtTokenProvider = jwtTokenProvider;
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

    // ─── CS 질문 시딩 ──────────────────────────────────────────────────────────

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

    // ─── 통계 시딩 (유저 생성 + 채점 기록 시딩 위임) ─────────────────────────

    public SeedStatsResult seedStats(int userCount, int days, int fromIndex) {
        // 1. 유저 생성
        SeedUsersResponse usersResp = userClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/users")
                        .queryParam("count", userCount)
                        .build())
                .retrieve()
                .body(SeedUsersApiResponse.class)
                .data();

        // 2. grading-service에 채점 기록 시딩 위임
        //    grading-service가 grading_logs 저장 + grading.completed 이벤트 발행을 일괄 처리한다.
        SeedGradingLogsResponse gradingResp = gradingClient.post()
                .uri(uri -> uri.path("/v1/internal/dev/grading-logs")
                        .queryParam("count", userCount)
                        .queryParam("days", days)
                        .queryParam("fromIndex", fromIndex)
                        .build())
                .retrieve()
                .body(SeedGradingLogsApiResponse.class)
                .data();

        return new SeedStatsResult(usersResp.created(), gradingResp.eventsPublished(), days);
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
    public record SeedStatsResult(int usersCreated, int gradingEventsPublished, int daysSimulated) {}
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

    private record SeedGradingLogsResponse(int logsCreated, int eventsPublished) {}
    private record SeedGradingLogsApiResponse(boolean isSuccess, SeedGradingLogsResponse data) {}

    private record UserExistsApiResponse(boolean isSuccess, Boolean data) {}
}
