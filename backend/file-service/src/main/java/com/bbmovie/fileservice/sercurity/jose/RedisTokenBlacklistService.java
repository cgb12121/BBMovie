package com.bbmovie.fileservice.sercurity.jose;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private final ReactiveStringRedisTemplate redisTemplate;

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    @Autowired
    public RedisTokenBlacklistService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Boolean> isBlacklisted(String jti) {
        return redisTemplate.hasKey("blacklist:" + jti);
    }

    @Override
    public Mono<Void> addTokenToBlacklist(String jti) {
        return redisTemplate.opsForValue().set("blacklist:" + jti, "", DEFAULT_TTL).then();
    }
}