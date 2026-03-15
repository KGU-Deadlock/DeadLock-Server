package com.deadlock.hellocs.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessValidity;
    private final long refreshValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessValidity,
            @Value("${jwt.refresh-token-expiration}") long refreshValidity) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessValidity = accessValidity;
        this.refreshValidity = refreshValidity;
    }

    public String createAccessToken(Authentication authentication, String role) {
        Date now = new Date();
        Date validityBy = new Date(now.getTime() + accessValidity);

        JwtBuilder builder = Jwts.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiration(validityBy)
                .signWith(key);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    public String createRefreshToken(Authentication authentication) {
        Date now = new Date();
        Date validityBy = new Date(now.getTime() + refreshValidity);

        return Jwts.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiration(validityBy)
                .signWith(key)
                .compact();
    }


    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).build();
    }
}