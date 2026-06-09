package com.deadlock.hellocs.quiz.adapter.out.external;

import com.deadlock.hellocs.quiz.application.port.out.QueryUserOutputPort;
import com.deadlock.hellocs.quiz.contract.QuizLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * user-service(또는 Step 3에서는 모놀리스)에서 사용자 레벨을 조회하는 REST 어댑터.
 * GET /v1/users/{kakaoId}/level → QuizLevel 문자열 반환.
 */
@Component
public class UserLevelRestAdapter implements QueryUserOutputPort {

    private final RestClient restClient;

    public UserLevelRestAdapter(@Value("${service.user.url}") String userServiceUrl) {
        this.restClient = RestClient.create(userServiceUrl);
    }

    @Override
    public QuizLevel getUserLevel(Long kakaoId) {
        LevelResponse resp = restClient.get()
                .uri("/v1/users/{kakaoId}/level", kakaoId)
                .retrieve()
                .body(LevelResponse.class);
        String level = (resp != null && resp.data() != null) ? resp.data() : QuizLevel.JUNIOR.name();
        return QuizLevel.valueOf(level);
    }

    private record LevelResponse(String data) {}
}
