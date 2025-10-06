package com.example.bbmoviesearch.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

//TODO: cache is super complex for kNN because high-dimensional data
@Log4j2
@Service
public class ReactiveCacheService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Autowired
    public ReactiveCacheService(ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    public <T> String buildCacheKey(String query, int page, int size, int age, String region, Class<T> clazz) {
        return String.format(
                "search:similar:q=%s:p=%d:s=%d:age=%d:reg=%s:type=%s",
                Base64.getUrlEncoder().encodeToString(query.getBytes(StandardCharsets.UTF_8)),
                page, size, age, region, clazz.getSimpleName()
        );
    }

    public <T> Mono<T> get(String key, Class<T> clazz) {
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .doOnNext(value -> log.debug("Cache hit for key: {}", key))
                .cast(clazz)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Cache miss for key: {}", key);
                    return Mono.empty();
                }));
    }

    public <T> Mono<Void> put(String key, T value, Duration ttl) {
        return reactiveRedisTemplate.opsForValue()
                .set(key, value, ttl)
                .doOnSuccess(ignored -> log.debug("Cache stored for key: {}", key))
                .then();
    }

    public Mono<Void> evict(String key) {
        return reactiveRedisTemplate.delete(key)
                .then();
    }
}
