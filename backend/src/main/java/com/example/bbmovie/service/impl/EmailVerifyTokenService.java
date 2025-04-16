package com.example.bbmovie.service.impl;

import com.example.bbmovie.model.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@Log4j2
public class EmailVerifyTokenService {
    private final RedisTemplate<Object, Object> redisTemplate;
    private final HashOperations<Object, String, String> hashOperations;
    private static final String VERIFICATION_TOKEN_PREFIX = "verification";

    public EmailVerifyTokenService(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    @PostConstruct
    public void testRedis() {
        redisTemplate.opsForValue().set("test:key", "Hello", Duration.ofMinutes(5));
        Object value = redisTemplate.opsForValue().get("test:key");
        log.info("Redis test value: {}", value);
    }

    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();

        hashOperations.put(VERIFICATION_TOKEN_PREFIX, user.getEmail(), token);

        log.info("Generated token {} for email {}", token, user.getEmail());
        log.info("Token TTL: {}", redisTemplate.getExpire(VERIFICATION_TOKEN_PREFIX + token));

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
} 