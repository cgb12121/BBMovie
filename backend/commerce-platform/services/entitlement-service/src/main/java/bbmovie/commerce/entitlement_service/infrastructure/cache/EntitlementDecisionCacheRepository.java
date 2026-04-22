package bbmovie.commerce.entitlement_service.infrastructure.cache;

import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementDecisionResponse;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;

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
            String pattern = prefix + "*";
            List<String> keysToDelete = redisTemplate.execute((RedisCallback<List<String>>) connection -> {
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
                List<String> found = new ArrayList<>();
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        found.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return found;
            });

            if (keysToDelete != null && !keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        } catch (DataAccessException ex) {
            log.debug("Failed evict entitlement cache prefix={}", prefix, ex);
        }
    }
}
