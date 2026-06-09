package com.deadlock.hellocs.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

@Slf4j
@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private static final String TYPE_ACCESS = "access";

    private static final List<String> PERMIT_ALL_PREFIXES = List.of(
            "/v1/auth",
            "/v1/dev",
            "/v1/ws",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars",
            "/actuator"
    );

    private static final List<String> PERMIT_GET_EXACT = List.of(
            "/v1/topics",
            "/v1/ranking/summary"
    );

    private final SecretKey key;

    public JwtAuthGatewayFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        // 클라이언트 스푸핑 방어: 인바운드 X-User-Id/X-Role 헤더를 항상 제거한 후 처리
        ServerHttpRequest stripped = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-Role");
                })
                .build();
        ServerWebExchange strippedExchange = exchange.mutate().request(stripped).build();

        if (isPermitted(path, method)) {
            return chain.filter(strippedExchange);
        }

        String authHeader = stripped.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeUnauthorized(exchange);
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_ACCESS.equals(claims.get("type", String.class))) {
                return writeUnauthorized(exchange);
            }

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            ServerHttpRequest mutated = stripped.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Role", role != null ? role : "USER")
                    .build();

            return chain.filter(strippedExchange.mutate().request(mutated).build());

        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return writeUnauthorized(exchange);
        }
    }

    private boolean isPermitted(String path, String method) {
        for (String prefix : PERMIT_ALL_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/") || path.startsWith(prefix + ".")) {
                return true;
            }
        }

        if ("GET".equalsIgnoreCase(method)) {
            for (String exact : PERMIT_GET_EXACT) {
                if (path.equals(exact)) return true;
            }
            if (path.startsWith("/v1/users/") &&
                    (path.endsWith("/interest-topic") || path.endsWith("/profile-summary"))) {
                return true;
            }
            if (path.equals("/v1/users/profile-summaries")) return true;
        }

        return false;
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        String body = "{\"isSuccess\":false,\"code\":\"AUTH_4001\",\"message\":\"인증이 필요합니다.\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
