package com.deadlock.hellocs.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity-seconds}")
    private long accessValidity;

    @Value("${jwt.refresh-token-validity-seconds}")
    private long refreshValidity;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] keyBytes;
        // secret이 Base64가 아니더라도 자동 처리 가능하게
        try { keyBytes = Decoders.BASE64.decode(secret); } catch (Exception e) { keyBytes = secret.getBytes(); }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessValidity)))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshValidity)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

