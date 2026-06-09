package com.deadlock.hellocs.dev.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class DevJwtTokenProvider {

    private final SecretKey key;
    private final long accessValidity;
    private final long refreshValidity;

    public DevJwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessValidity,
            @Value("${jwt.refresh-token-expiration}") long refreshValidity) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessValidity = accessValidity;
        this.refreshValidity = refreshValidity;
    }

    public String createAccessToken(String subject, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("type", "access")
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessValidity))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String subject) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshValidity))
                .signWith(key)
                .compact();
    }
}
