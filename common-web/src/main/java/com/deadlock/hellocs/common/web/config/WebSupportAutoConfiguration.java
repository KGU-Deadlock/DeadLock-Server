package com.deadlock.hellocs.common.web.config;

import com.deadlock.hellocs.common.web.exception.GlobalExceptionHandler;
import com.deadlock.hellocs.common.web.resolver.CurrentUserArgumentResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * common-web 모듈 공통 웹 인프라 Auto-Configuration.
 *
 * - {@link GlobalExceptionHandler}: 전역 예외 처리 (CustomException → ApiResponse)
 * - {@link CurrentUserArgumentResolver}: @CurrentUser 파라미터 해석 (X-User-Id 헤더 → Long/CurrentUserInfo)
 */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebSupportAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
