package com.deadlock.hellocs.common.web.resolver;

/**
 * 게이트웨이가 주입한 사용자 정보.
 * X-User-Id, X-Role 헤더 값을 담는 경량 값 객체.
 *
 * @param userId kakaoId (X-User-Id 헤더)
 * @param role   사용자 역할 (X-Role 헤더, 없으면 "USER")
 */
public record CurrentUserInfo(Long userId, String role) {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ROLE = "X-Role";
    public static final String DEFAULT_ROLE = "USER";
}
