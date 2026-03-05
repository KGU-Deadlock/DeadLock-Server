package com.deadlock.hellocs.global.config;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 수정 표시
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 수정 표시
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        // 수정 표시
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/api/v1/auth/token/**").permitAll()
                        .requestMatchers("/api/v1/dev/**").permitAll()
                        // 수정 표시
                        .requestMatchers(HttpMethod.GET, "/api/v1/ranking/summary").permitAll()
                        // 수정 표시
                        .requestMatchers(HttpMethod.GET, "/api/v1/ranking").authenticated()
                        // 수정 표시
                        .requestMatchers(HttpMethod.GET, "/api/v1/streak").authenticated()
                        // 수정 표시
                        .requestMatchers(HttpMethod.GET, "/api/v1/streak/detail").authenticated()
                        // 수정 표시
                        .requestMatchers("/api/v1/quiz/**").authenticated()
                        // 수정 표시
                        .requestMatchers("/api/v1/quiz/grading/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .redirectionEndpoint(redirection -> redirection.baseUri("/api/v1/auth/token"))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, ex1) -> {
                    ErrorStatus errorStatus = ErrorStatus._UNAUTHORIZED;
                    res.setStatus(errorStatus.getReasonHttpStatus().getHttpStatus().value());
                    res.setContentType("application/json;charset=UTF-8");
                    try (PrintWriter w = res.getWriter()) {
                        w.write(String.format(
                                "{\"isSuccess\":false,\"code\":\"%s\",\"message\":\"%s\",\"data\":null}",
                                errorStatus.getCode(),
                                errorStatus.getMessage()));
                    }
                })
                .accessDeniedHandler((req, res, ex2) -> {
                    ErrorStatus errorStatus = ErrorStatus._FORBIDDEN;
                    res.setStatus(errorStatus.getReasonHttpStatus().getHttpStatus().value());
                    res.setContentType("application/json;charset=UTF-8");
                    try (PrintWriter w = res.getWriter()) {
                        w.write(String.format(
                                "{\"isSuccess\":false,\"code\":\"%s\",\"message\":\"%s\",\"data\":null}",
                                errorStatus.getCode(),
                                errorStatus.getMessage()));
                    }
                }));

        return http.build();
    }
}
