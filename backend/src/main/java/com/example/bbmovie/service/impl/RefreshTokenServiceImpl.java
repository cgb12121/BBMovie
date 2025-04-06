package com.example.bbmovie.service.impl;

import com.example.bbmovie.service.intf.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);

    @Override
    public String createRefreshToken(String username) {
        String tokenId = UUID.randomUUID().toString();
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        
        redisTemplate.opsForValue().set(key, username, REFRESH_TOKEN_EXPIRY);
        
        return tokenId;
    }

    @Override
    public String getUsernameFromRefreshToken(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        return (String) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteRefreshToken(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        redisTemplate.delete(key);
    }

    @Override
    public boolean isValidRefreshToken(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
} 