package com.example.bbmovie.service.email;

import com.example.bbmovie.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Log4j2
public class ChangePasswordTokenService {

    private final RedisTemplate<Object, Object> redisTemplate;
    private static final String CHANGE_PASSWORD_TOKEN_PREFIX = "changePassword:token:";
    private static final String TOKEN_TO_EMAIL_PREFIX = "changePassword:email:";

    @Value("${app.change-password-token-expiration-minutes}")
    private long tokenExpirationMinutes;

    public ChangePasswordTokenService(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateChangePasswordToken(User user) {
        String token = UUID.randomUUID().toString();
        String emailKey = CHANGE_PASSWORD_TOKEN_PREFIX + user.getEmail();
        String tokenKey = TOKEN_TO_EMAIL_PREFIX + token;

        redisTemplate.opsForValue().set(emailKey, token, tokenExpirationMinutes, TimeUnit.MINUTES);
        log.info("Change password token has been created {}, {}", emailKey, token);

        redisTemplate.opsForValue().set(tokenKey, user.getEmail(), tokenExpirationMinutes, TimeUnit.MINUTES);

        log.info("Generated change password token {} for email {}", token, user.getEmail());
        Long ttl = redisTemplate.getExpire(emailKey, TimeUnit.SECONDS);
        log.info("Token TTL: {} seconds", ttl);

        return token;
    }

    @Cacheable(value = "changePassword", key = "#token")
    public String getEmailForToken(String token) {
        String tokenKey = TOKEN_TO_EMAIL_PREFIX + token;
        Object email = redisTemplate.opsForValue().get(tokenKey);

        log.info("Get email for token {} for email {}", token, email);
        return email != null ? email.toString() : null;
    }

    @CacheEvict(value = "changePassword", key = "#token")
    public void deleteToken(String token) {
        String tokenKey = TOKEN_TO_EMAIL_PREFIX + token;
        Object email = redisTemplate.opsForValue().get(tokenKey);
        log.info("Delete email for token {} for email {}", token, email);
        if (email != null) {
            String emailKey = CHANGE_PASSWORD_TOKEN_PREFIX + email;
            redisTemplate.delete(emailKey);
            redisTemplate.delete(tokenKey);
        }
    }
}