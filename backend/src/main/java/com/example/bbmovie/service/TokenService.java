package com.example.bbmovie.service;

import com.example.bbmovie.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String VERIFICATION_TOKEN_PREFIX = "verification:";
    private static final Duration TOKEN_EXPIRY = Duration.ofHours(1);

    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        String key = VERIFICATION_TOKEN_PREFIX + token;
        
        // Store user ID and email in Redis with TTL
        redisTemplate.opsForValue().set(key, user.getEmail(), TOKEN_EXPIRY);
        
        return token;
    }

    public String getEmailForToken(String token) {
        String key = VERIFICATION_TOKEN_PREFIX + token;
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void deleteToken(String token) {
        String key = VERIFICATION_TOKEN_PREFIX + token;
        redisTemplate.delete(key);
    }

    public boolean isValidToken(String token) {
        String key = VERIFICATION_TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public long getTokenExpirySeconds(String token) {
        String key = VERIFICATION_TOKEN_PREFIX + token;
        return redisTemplate.getExpire(key);
    }
} 