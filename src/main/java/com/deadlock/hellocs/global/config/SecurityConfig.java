package com.deadlock.hellocs.global.config;

import com.deadlock.hellocs.global.apiPayload.code.status.ErrorStatus;
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

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.build();
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
