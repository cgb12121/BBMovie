package com.example.bbmovie.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class JwtTokenProvider {

    private final int jwtExpirationInMs;
    private final int jwtRefreshExpirationInMs;
    private final SecretKey key;
    private final RedisTemplate<Object, Object> redisTemplate;

    public JwtTokenProvider(
            @Value("${app.jwt-secret}") String jwtSecret,
            @Value("${app.jwt-expiration-milliseconds}") int jwtExpirationInMs,
            @Value("${app.jwt-refresh-expiration-milliseconds}") int jwtRefreshExpirationInMs,
            RedisTemplate<Object, Object> redisTemplate
    ) {
        this.jwtExpirationInMs = jwtExpirationInMs;
        this.jwtRefreshExpirationInMs = jwtRefreshExpirationInMs;
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String accessToken = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
        log.info("\n\t Generated access token for {}: \n\tToken: {}\n\tExpiration:{}",
                getUsernameFromToken(accessToken),
                accessToken,
                getExpirationDateFromToken(accessToken)
        );
        return accessToken;
    }

    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationInMs);

        String refreshToken = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();

        log.info("\n\t Generated refresh token for {}: \n\tToken: {}\n\tExpiration:{}",
                getUsernameFromToken(refreshToken),
                refreshToken,
                getExpirationDateFromToken(refreshToken)
        );
        return refreshToken;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            log.error("Token validation error: {}", ex.getMessage());
        }
        return false;
    }

    public void invalidateToken(String accessToken) {
        String blacklistKey = "blacklist:" + accessToken;
        redisTemplate.opsForValue().set(
            blacklistKey,
            "invalidated",
            getExpirationDateFromToken(accessToken).getTime() - System.currentTimeMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}