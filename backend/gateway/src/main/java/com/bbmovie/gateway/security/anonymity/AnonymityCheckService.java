package com.bbmovie.gateway.security.anonymity;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@Log4j2
public class AnonymityCheckService {

    private final List<IpAnonymityProvider> providers;
    private final ReactiveRedisTemplate<String, Boolean> redisTemplate;

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String CACHE_KEY_PREFIX = "anonymity_check:";

    public AnonymityCheckService(
            List<IpAnonymityProvider> providers,
            @Qualifier("ipRedisReactive") ReactiveRedisTemplate<String, Boolean> redisTemplate
    ) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("Anonymity providers list cannot be null or empty.");
        }
        this.providers = providers;
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isAnonymous(String ip) {
        if (providers.isEmpty()) {
            log.warn("No IP anonymity providers configured. Defaulting to non-anonymous.");
            return Mono.just(false);
        }

        if ("127.0.0.1".equals(ip)) {
            return Mono.just(false);
        }

        String cacheKey = CACHE_KEY_PREFIX + ip;

        return redisTemplate.opsForValue().get(cacheKey)
                .doOnSuccess(cachedResult -> {
                    if (cachedResult != null) {
                        log.info("Returning cached anonymity status for IP: {} -> {}", ip, cachedResult);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("IP {} not found in cache. Performing API checks.", ip);
                    return checkProviders(ip)
                            .flatMap(result -> redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL).thenReturn(result))
                            .doOnSuccess(result -> log.info("Cached anonymity status for IP: {} -> {} with TTL {} hours", ip, result, CACHE_TTL.toHours()));
                }));
    }

    private Mono<Boolean> checkProviders(String ip) {
        return Flux.fromIterable(providers)
                .concatMap(provider -> provider.isAnonymity(ip)
                        .doOnSuccess(result -> log.info("Checked with provider: {}. Result for IP {}: {}", provider.getName(), ip, result))
                        .onErrorResume(e -> {
                            log.error("Provider {} failed for IP {}: {}. Moving to the next provider.",
                                    provider.getName(), ip, e.getMessage());
                            return Mono.just(false); // Treat failure as non-anonymous and continue
                        }))
                .filter(isAnonymous -> isAnonymous) // Find the first provider that returns true
                .next() // Take the first true result
                .defaultIfEmpty(false); // If no provider returns true, default to false
    }
}