package com.deadlock.hellocs.user.auth.jwt;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성 및 refresh 토큰 검증 컴포넌트.
 * 토큰 발급은 user-service가 단독 담당하며, 다른 서비스는 이 빈을 가지지 않는다.
 * 게이트웨이는 자체 jjwt 파서로 access 토큰을 검증하므로 이 빈을 사용하지 않는다.
 */
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessValidity;
    @Getter
    private final long refreshValidity;
    private final NimbusJwtDecoder baseDecoder;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessValidity,
            @Value("${jwt.refresh-token-expiration}") long refreshValidity) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessValidity = accessValidity;
        this.refreshValidity = refreshValidity;
        this.baseDecoder = NimbusJwtDecoder.withSecretKey(key).build();
    }

    public String createAccessToken(String subject, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessValidity);

        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    public String createRefreshToken(String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshValidity);

        return Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public NimbusJwtDecoder baseJwtDecoder() {
        return baseDecoder;
    }

    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(jwt -> {
            if (TYPE_ACCESS.equals(jwt.getClaimAsString(CLAIM_TYPE))) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Not an access token", null));
        });
        return decoder;
    }
}
