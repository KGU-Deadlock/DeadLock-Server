package com.deadlock.hellocs.user.config;

import com.deadlock.hellocs.user.auth.handler.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * user-service 전용 SecurityFilterChain.
 *
 * 게이트웨이가 인증/인가를 전담하므로, 이 서비스의 보안 역할은
 * Kakao OAuth2 로그인 플로우 (토큰 발급) 처리에 한정된다.
 * 비즈니스 엔드포인트(/v1/users/**)의 사용자 식별은 @CurrentUser (X-User-Id 헤더)로 처리한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(SessionManagementConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authz -> authz.baseUri("/v1/auth/oauth2"))
                        .redirectionEndpoint(redirection -> redirection.baseUri("/v1/auth/token"))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .build();
    }
}
