package com.deadlock.hellocs.common.web.resolver;

import com.deadlock.hellocs.common.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.common.exception.CustomException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUser} 애너테이션이 붙은 컨트롤러 파라미터를 해석한다.
 *
 * <ul>
 *   <li>파라미터 타입이 {@link Long}이면 X-User-Id 헤더 값을 Long으로 변환해 반환한다.</li>
 *   <li>파라미터 타입이 {@link CurrentUserInfo}이면 X-User-Id + X-Role을 담은 레코드를 반환한다.</li>
 * </ul>
 *
 * X-User-Id 헤더가 없으면 {@code UNAUTHORIZED} 예외를 던진다.
 * 게이트웨이는 인증된 요청에만 이 헤더를 주입하므로, 없다는 것은 게이트웨이를 우회했거나 허가되지 않은 요청이다.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && (parameter.getParameterType().equals(Long.class)
                        || parameter.getParameterType().equals(CurrentUserInfo.class));
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        String userIdHeader = webRequest.getHeader(CurrentUserInfo.HEADER_USER_ID);

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new CustomException(ErrorStatus._UNAUTHORIZED);
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdHeader);
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorStatus._UNAUTHORIZED);
        }

        if (parameter.getParameterType().equals(Long.class)) {
            return userId;
        }

        String roleHeader = webRequest.getHeader(CurrentUserInfo.HEADER_ROLE);
        String role = (roleHeader != null && !roleHeader.isBlank()) ? roleHeader : CurrentUserInfo.DEFAULT_ROLE;
        return new CurrentUserInfo(userId, role);
    }
}
