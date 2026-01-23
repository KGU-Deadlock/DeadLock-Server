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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http.build();
    }

    @Bean
    Customizer<HttpSecurity> oauth2Customizer() {
        return http -> http
                .oauth2Login((oauth2) -> oauth2
                        .redirectionEndpoint((redirection) -> redirection
                                .baseUri("/api/v1/auth/token")) // 서버가 이 경로로 인가 Code 받아서 Token 받아옴
                        .successHandler(oAuth2LoginSuccessHandler)
                );
    }

    @Bean
    Customizer<HttpSecurity> jwtCustomizer() {
        return http -> http
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));
    }

    @Bean
    Customizer<HttpSecurity> defaultRestApiSetting(){
        return http -> http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable);
    }

    @Bean
    Customizer<HttpSecurity> exceptionHandler(){
        return http -> http.exceptionHandling(ex -> ex
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
    }
}
