package com.deadlock.hellocs.global.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * JWT가 필요한 보호 경로에만 동작하도록 만든 필터.
 * - 화이트리스트(permitAll) 경로와 OPTIONS 프리플라이트는 필터를 건너뜀.
 * - 토큰이 유효하면 SecurityContext 설정, 아니면 체인 진행 (401은 EntryPoint가 처리)
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final List<String> skipPatterns;     // 필터를 스킵할 경로 패턴들(ant style)
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtTokenProvider provider, Collection<String> skipPatterns) {
        this.jwtTokenProvider = provider;
        this.skipPatterns = skipPatterns == null ? List.of() : List.copyOf(skipPatterns);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 1) CORS preflight는 항상 스킵
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // 2) 화이트리스트 패턴은 스킵
        String path = request.getServletPath();
        for (String p : skipPatterns) {
            if (matcher.match(p, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                String subject = claims.getSubject();
                if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 필요 시 roles/authorities를 claims에서 꺼내서 넣어도 됨
                    var auth = new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // 유효하지 않으면 그냥 통과 -> 최종적으로 EntryPoint가 401 응답 처리
            }
        }

        chain.doFilter(request, response);
    }
}

