package com.deadlock.hellocs.global.client;

import com.deadlock.hellocs.global.client.dto.UserDetail;
import com.deadlock.hellocs.global.client.dto.UserSummaryResult;
import com.deadlock.hellocs.global.client.port.UserDetailPort;
import com.deadlock.hellocs.global.client.port.UserSummaryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * core-service의 /internal/v1/users 엔드포인트를 호출하는 어댑터.
 */
@Component
@RequiredArgsConstructor
public class CoreServiceUserAdapter implements UserSummaryPort, UserDetailPort {

    private final RestClient restClient;

    @Value("${core-service.base-url}")
    private String coreServiceBaseUrl;

    @Override
    public UserSummaryResult getUserSummary(Long kakaoId) {
        return restClient.get()
                .uri(coreServiceBaseUrl + "/internal/v1/users/summary?kakaoId=" + kakaoId)
                .retrieve()
                .body(UserSummaryResult.class);
    }

    @Override
    public List<UserSummaryResult> getUserSummaries(List<Long> kakaoIds) {
        String ids = kakaoIds.stream().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse("");
        return restClient.get()
                .uri(coreServiceBaseUrl + "/internal/v1/users/summaries?kakaoIds=" + ids)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @Override
    public UserDetail getUserDetail(Long kakaoId) {
        return restClient.get()
                .uri(coreServiceBaseUrl + "/internal/v1/users/" + kakaoId + "/detail")
                .retrieve()
                .body(UserDetail.class);
    }

    @Override
    public List<UserDetail> getUserDetails(List<Long> kakaoIds) {
        return restClient.post()
                .uri(coreServiceBaseUrl + "/internal/v1/users/details")
                .body(kakaoIds)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
