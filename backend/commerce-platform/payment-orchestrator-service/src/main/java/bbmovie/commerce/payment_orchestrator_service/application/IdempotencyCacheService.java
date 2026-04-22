package bbmovie.commerce.payment_orchestrator_service.application;

import bbmovie.commerce.payment_orchestrator_service.application.config.IdempotencyOperation;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.json.JsonSerdeUtils;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class IdempotencyCacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final long ttlSeconds;

    public IdempotencyCacheService(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.idempotency.cache.enabled:true}") boolean enabled,
            @Value("${app.idempotency.cache.ttl-seconds:900}") long ttlSeconds
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.ttlSeconds = ttlSeconds;
    }

    public <T> Optional<T> get(IdempotencyOperation op, String key, Class<T> responseType) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            String json = redis.opsForValue().get(cacheKey(op, key));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(JsonSerdeUtils.read(objectMapper, json, responseType, "Failed to deserialize JSON"));
        } catch (Exception e) {
            log.warn("Idempotency cache read failed, fallback to DB");
            return Optional.empty();
        }
    }

    public void put(IdempotencyOperation op, String key, Object response) {
        if (!enabled) {
            return;
        }
        try {
            String json = JsonSerdeUtils.write(objectMapper, response, "Failed to serialize JSON");
            redis.opsForValue().set(cacheKey(op, key), json, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Idempotency cache write failed, DB remains source of truth");
        }
    }

    private String cacheKey(IdempotencyOperation op, String key) {
        return "po:idempotency:" + op.name() + ":" + key;
    }
}

