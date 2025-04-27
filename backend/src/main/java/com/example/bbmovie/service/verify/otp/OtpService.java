package com.example.bbmovie.service.verify.otp;

import com.example.bbmovie.entity.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class OtpService {

    private final RedisTemplate<Object, Object> redisTemplate;
    private static final String OTP_TOKEN_PREFIX = "otp:token:";
    private static final String TOKEN_TO_EMAIL_PREFIX = "otp:email:";

    @Value("${app.otp-expiration-minutes}")
    private long otpExpirationMinutes;

    public OtpService(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public String generateOtpToken(User user) {
        SecureRandom secureRandom = new SecureRandom();
        String otp = String.format("%06d", secureRandom.nextInt(999999));
        String emailKey = OTP_TOKEN_PREFIX + user.getEmail();
        String tokenKey = TOKEN_TO_EMAIL_PREFIX + otp;

        redisTemplate.opsForValue().set(emailKey, otp, otpExpirationMinutes, TimeUnit.MINUTES);
        log.info("OTP token has been created {}, {}", emailKey, otp);

        redisTemplate.opsForValue().set(tokenKey, user.getEmail(), otpExpirationMinutes, TimeUnit.MINUTES);

        log.info("Generated OTP token {} for email {}", otp, user.getEmail());
        Long ttl = redisTemplate.getExpire(emailKey, TimeUnit.SECONDS);
        log.info("Token TTL: {} seconds", ttl);

        return otp;
    }

    @Cacheable(value = "otp", key = "#token")
    public String getEmailForOtpToken(String token) {
        String tokenKey = TOKEN_TO_EMAIL_PREFIX + token;
        Object email = redisTemplate.opsForValue().get(tokenKey);

        log.info("Get email for OTP token {} for email {}", token, email);
        return email != null ? email.toString() : null;
    }

    @CacheEvict(value = "otp", key = "#token")
    public void deleteOtpToken(String token) {
        String tokenKey = TOKEN_TO_EMAIL_PREFIX + token;
        Object email = redisTemplate.opsForValue().get(tokenKey);
        log.info("Delete email for OTP token {} for email {}", token, email);
        if (email != null) {
            String emailKey = OTP_TOKEN_PREFIX + email;
            redisTemplate.delete(emailKey);
            redisTemplate.delete(tokenKey);
        }
    }
}
