package com.deadlock.hellocs.global.auth.client;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class KakaoAuthApiClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String frontRedirectUri;

    public KakaoAuthApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.client-secret}") String clientSecret,
            @Value("${kakao.front-redirect-uri}") String frontRedirectUri) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.frontRedirectUri = frontRedirectUri;
    }

    public String exchangeCodeForToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", frontRedirectUri);
        params.add("code", code);

        try {
            Map<?, ?> response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new CustomException(ErrorStatus._INVALID_KAKAO_CODE);
            }
            return (String) response.get("access_token");
        } catch (RestClientException e) {
            throw new CustomException(ErrorStatus._INVALID_KAKAO_CODE);
        }
    }

    @SuppressWarnings("unchecked")
    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        try {
            Map<String, Object> response = (Map<String, Object>) restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new CustomException(ErrorStatus._INVALID_KAKAO_CODE);
            }

            Long kakaoId = Long.valueOf(response.get("id").toString());
            Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            String nickname = (String) profile.get("nickname");

            return new KakaoUserInfo(kakaoId, nickname);
        } catch (RestClientException e) {
            throw new CustomException(ErrorStatus._INVALID_KAKAO_CODE);
        }
    }

    public record KakaoUserInfo(Long kakaoId, String nickname) {}
}