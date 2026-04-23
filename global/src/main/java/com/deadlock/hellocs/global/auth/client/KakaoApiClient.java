package com.deadlock.hellocs.global.auth.client;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.exception.CustomException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoApiClient {

    private static final String KAPI_BASE = "https://kapi.kakao.com";

    private final RestClient restClient;
    private final Long kakaoAppId;

    public KakaoApiClient(@Value("${kakao.app-id}") Long kakaoAppId) {
        this.restClient = RestClient.create(KAPI_BASE);
        this.kakaoAppId = kakaoAppId;
    }

    /**
     * 카카오 액세스 토큰의 유효성을 검증하고 자체 앱 토큰인지 확인합니다.
     * 타 앱 토큰으로 로그인하는 것을 방지합니다.
     */
    public void validateToken(String accessToken) {
        KakaoTokenInfo tokenInfo = restClient.get()
                .uri("/v1/user/access_token_info")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> status == HttpStatus.UNAUTHORIZED,
                        (req, res) -> { throw new CustomException(ErrorStatus._INVALID_KAKAO_TOKEN); })
                .body(KakaoTokenInfo.class);

        if (tokenInfo == null || !kakaoAppId.equals(tokenInfo.appId())) {
            throw new CustomException(ErrorStatus._INVALID_KAKAO_TOKEN);
        }
    }

    /**
     * 카카오 사용자 정보를 조회합니다.
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        KakaoUserInfo userInfo = restClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> status == HttpStatus.UNAUTHORIZED,
                        (req, res) -> { throw new CustomException(ErrorStatus._INVALID_KAKAO_TOKEN); })
                .body(KakaoUserInfo.class);

        if (userInfo == null) {
            throw new CustomException(ErrorStatus._INVALID_KAKAO_TOKEN);
        }
        return userInfo;
    }

    public record KakaoTokenInfo(
            Long id,
            @JsonProperty("expires_in") Integer expiresIn,
            @JsonProperty("app_id") Long appId
    ) {}

    public record KakaoUserInfo(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
        public record KakaoAccount(
                Profile profile
        ) {
            public record Profile(String nickname) {}
        }
    }
}
