package com.deadlock.hellocs.global.config;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
import com.deadlock.hellocs.global.security.jwt.JwtAuthenticationFilter;
import com.deadlock.hellocs.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;

  // Swagger
  private static final String[] SWAGGER = {
      "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
  };

  // 로그인/토큰 교환/리다이렉트/헬스체크 등 공개 경로
  private static final String[] PUBLIC = {
      "/", "/actuator/health",
      "/api/auth/**", // 카카오 코드 교환 API 등
      "/oauth2/**",
      "/login/**", "/login/oauth2/**",
      "/api/test/auth/**",
  };

  // 정적 리소스
  private static final String[] STATIC = {
      "/favicon.ico", "/assets/**", "/css/**", "/js/**", "/images/**"
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    // Jwt 필터에서 건너뛸(스킵) 경로 패턴 통합
    List<String> skip = new ArrayList<>();
    addAll(skip, SWAGGER);
    addAll(skip, PUBLIC);
    addAll(skip, STATIC);

    JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider, skip);

    http
        // REST API 기본 세팅
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .logout(lo -> lo.disable())
        .requestCache(cache -> cache.disable())

        // 권한 규칙
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight 허용
            .requestMatchers(SWAGGER).permitAll()
            .requestMatchers(PUBLIC).permitAll()
            .requestMatchers(STATIC).permitAll()
            .anyRequest().authenticated())

        // 인증/인가 실패 공통 응답(JSON) - ApiResponse 형식
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
            }))

        // JWT 필터 등록(UsernamePasswordAuthenticationFilter 앞)
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  private static void addAll(List<String> target, String[] arr) {
    for (String s : arr)
      target.add(s);
  }

  // CORS (개발용: 필요 시 도메인 고정/축소)
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:5173","http://34.158.222.233:8080","https://connecting-family.vercel.app"));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    c.setExposedHeaders(List.of("Authorization", "Location"));
    c.setAllowCredentials(true);
    c.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
