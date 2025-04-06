package com.example.bbmovie.service.impl;

import com.example.bbmovie.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class EmailVerifyTokenService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String VERIFICATION_TOKEN_PREFIX = "verification:";
    private static final Duration TOKEN_EXPIRY = Duration.ofHours(1);

    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        String key = VERIFICATION_TOKEN_PREFIX + token;
        
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
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception ex) {
            log.error(ex);
            return 0;
        }
    }
} 