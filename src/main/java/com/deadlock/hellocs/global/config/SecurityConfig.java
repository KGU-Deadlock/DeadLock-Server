package com.deadlock.hellocs.global.config;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.PrintWriter;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "https://localhost:5173", "https://hellocs.site"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    Customizer<HttpSecurity> corsCustomizer() {
        return http -> http.cors(Customizer.withDefaults());
    }

    @Bean
    Customizer<HttpSecurity> defaultRestApiSetting() {
        return http -> http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(SessionManagementConfigurer::disable);
    }

    @Bean
    Customizer<HttpSecurity> jwtCustomizer() {
        return http -> http
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    }

    @Bean
    Customizer<HttpSecurity> authorizeCustomizer() {
        return http -> http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        .requestMatchers("/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/topics").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/ranking/summary").permitAll()
                        .requestMatchers("/v1/dev/**").permitAll()
                        .requestMatchers("/v1/ws/**").permitAll()
                        .requestMatchers("/v1/users/**").authenticated()
                        .requestMatchers("/v1/quiz/**").authenticated()
                        .requestMatchers("/v1/ranking/**").authenticated()
                        .requestMatchers("/v1/streak/**").authenticated()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().denyAll()
                );
    }

    @Bean
    Customizer<HttpSecurity> exceptionCustomizer() {
        return http -> http
                .exceptionHandling(ex -> ex
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
                        })
                );
    }
}
