package com.example.bbmovie.security.anonymity;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Log4j2
public class AnonymityCheckService {

    private final List<IpAnonymityProvider> providers;
    private final int numberOfProviders;
    private final AtomicInteger currentProviderIndex;

    private final RedisTemplate<String, Boolean> redisTemplate;

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String CACHE_KEY_PREFIX = "anonymity_check:";

    public AnonymityCheckService(
            List<IpAnonymityProvider> providers,
            @Qualifier("ipRedis") RedisTemplate<String, Boolean> redisTemplate
    ) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("Anonymity providers list cannot be null or empty.");
        }
        this.providers = providers;
        this.numberOfProviders = providers.size();
        this.currentProviderIndex = new AtomicInteger(0);
        this.redisTemplate = redisTemplate;
    }

    public boolean isAnonymous(String ip) {
        if (providers.isEmpty()) {
            log.warn("No IP anonymity providers configured. Defaulting to non-anonymous.");
            return false;
        }

        String cacheKey = CACHE_KEY_PREFIX + ip;
        Boolean cachedResult = redisTemplate.opsForValue().get(cacheKey);

        if (cachedResult != null) {
            log.info("Returning cached anonymity status for IP: {} -> {}", ip, cachedResult);
            return cachedResult;
        }

        log.info("IP {} not found in cache. Performing API checks.", ip);

        for (int attempt = 0; attempt < numberOfProviders; attempt++) {
            int indexToUse = currentProviderIndex.get();
            int actualIndex = indexToUse % numberOfProviders;

            IpAnonymityProvider providerToUse = providers.get(actualIndex);

            try {
                boolean result = providerToUse.isAnonymity(ip);
                log.info("Checked with provider: {}. Result for IP {}: {}", providerToUse.getName(), ip, result);

                redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL);
                log.info("Cached anonymity status for IP: {} -> {} with TTL {} hours", ip, result, CACHE_TTL.toHours());

                return result;
            } catch (Exception e) {
                log.error("Provider {} failed for IP {}: {}. Moving to the next provider.",
                        providerToUse.getName(), ip, e.getMessage()
                );
                currentProviderIndex.incrementAndGet();
            }
        }

        // All providers failed after attempting each once
        log.error("All IP anonymity providers failed for IP: {}. No result could be obtained.", ip);
        // Optionally cache a "failed to check" status, or return default, or throw.
        // For simplicity, we'll return false and NOT cache, as it's an indeterminate state.
        return false;
    }
}