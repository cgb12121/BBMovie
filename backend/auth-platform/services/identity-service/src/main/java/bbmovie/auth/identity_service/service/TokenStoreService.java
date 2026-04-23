package bbmovie.auth.identity_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TokenStoreService {
    private final StringRedisTemplate redisTemplate;

    @Value("${app.verification-token-expiration-minutes:15}")
    private long verificationTokenMinutes;

    @Value("${app.change-password-token-expiration-minutes:15}")
    private long resetTokenMinutes;

    private final Map<String, String> fallbackVerify = new ConcurrentHashMap<>();
    private final Map<String, String> fallbackReset = new ConcurrentHashMap<>();

    public String createVerificationToken(String email) {
        String token = UUID.randomUUID().toString();
        String tokenKey = "identity:verification:token:" + token;
        String emailKey = "identity:verification:email:" + email;
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(tokenKey, email, Duration.ofMinutes(verificationTokenMinutes));
            redisTemplate.opsForValue().set(emailKey, token, Duration.ofMinutes(verificationTokenMinutes));
        } else {
            fallbackVerify.put(tokenKey, email);
            fallbackVerify.put(emailKey, token);
        }
        return token;
    }

    public String getVerificationEmail(String token) {
        String tokenKey = "identity:verification:token:" + token;
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().get(tokenKey);
        }
        return fallbackVerify.get(tokenKey);
    }

    public void deleteVerificationToken(String token) {
        String tokenKey = "identity:verification:token:" + token;
        if (redisTemplate != null) {
            String email = redisTemplate.opsForValue().get(tokenKey);
            redisTemplate.delete(tokenKey);
            if (email != null) {
                redisTemplate.delete("identity:verification:email:" + email);
            }
        } else {
            String email = fallbackVerify.remove(tokenKey);
            if (email != null) {
                fallbackVerify.remove("identity:verification:email:" + email);
            }
        }
    }

    public String createResetToken(String email) {
        String token = UUID.randomUUID().toString();
        String tokenKey = "identity:reset:token:" + token;
        String emailKey = "identity:reset:email:" + email;
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(tokenKey, email, Duration.ofMinutes(resetTokenMinutes));
            redisTemplate.opsForValue().set(emailKey, token, Duration.ofMinutes(resetTokenMinutes));
        } else {
            fallbackReset.put(tokenKey, email);
            fallbackReset.put(emailKey, token);
        }
        return token;
    }

    public String getResetEmail(String token) {
        String tokenKey = "identity:reset:token:" + token;
        if (redisTemplate != null) {
            return redisTemplate.opsForValue().get(tokenKey);
        }
        return fallbackReset.get(tokenKey);
    }

    public void deleteResetToken(String token) {
        String tokenKey = "identity:reset:token:" + token;
        if (redisTemplate != null) {
            String email = redisTemplate.opsForValue().get(tokenKey);
            redisTemplate.delete(tokenKey);
            if (email != null) {
                redisTemplate.delete("identity:reset:email:" + email);
            }
        } else {
            String email = fallbackReset.remove(tokenKey);
            if (email != null) {
                fallbackReset.remove("identity:reset:email:" + email);
            }
        }
    }
}
