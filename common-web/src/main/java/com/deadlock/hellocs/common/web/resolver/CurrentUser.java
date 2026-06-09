package com.deadlock.hellocs.common.web.resolver;

import java.lang.annotation.*;

/**
 * 게이트웨이가 주입한 X-User-Id 헤더를 컨트롤러 파라미터로 바인딩한다.
 *
 * <pre>
 *   // Long으로 직접 받기
 *   public ApiResponse<...> myMethod(@CurrentUser Long userId) { ... }
 *
 *   // 역할 정보까지 필요한 경우
 *   public ApiResponse<...> myMethod(@CurrentUser CurrentUserInfo user) { ... }
 * </pre>
 *
 * X-User-Id 헤더가 없으면 {@code CustomException(ErrorStatus._UNAUTHORIZED)}를 던진다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
