package bbmovie.commerce.entitlement_service.infrastructure.cache;

import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementDecisionResponse;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EntitlementDecisionCacheRepository {
    private static final Duration TTL = Duration.ofMinutes(5);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<EntitlementDecisionResponse> get(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(raw, EntitlementDecisionResponse.class));
        } catch (Exception ex) {
            log.debug("Failed reading entitlement cache key={}", key, ex);
            return Optional.empty();
        }
    }

    public void put(String key, EntitlementDecisionResponse value) {
        try {
            String raw = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, raw, TTL);
        } catch (Exception ex) {
            log.debug("Failed writing entitlement cache key={}", key, ex);
        }
    }

    public void evictPrefix(String prefix) {
        try {
            var keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.debug("Failed evict entitlement cache prefix={}", prefix, ex);
        }
    }
}
