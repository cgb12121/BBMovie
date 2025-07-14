package com.bbmovie.fileservice.sercurity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    public RedisTokenBlacklistService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Boolean> isBlacklisted(String jti) {
        return redisTemplate.hasKey("blacklist:" + jti);
    }
}
