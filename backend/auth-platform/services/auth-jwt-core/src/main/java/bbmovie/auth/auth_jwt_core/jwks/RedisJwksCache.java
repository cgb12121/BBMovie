package bbmovie.auth.auth_jwt_core.jwks;

import bbmovie.auth.auth_jwt_core.model.CachedJwks;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class RedisJwksCache implements JwksCache {
    private static final String FIELD_JWKS_JSON = "jwksJson";
    private static final String FIELD_FETCHED_AT = "fetchedAt";
    private static final String FIELD_EXPIRES_AT = "expiresAt";

    private final StringRedisTemplate redisTemplate;
    private final String cacheKey;

    public RedisJwksCache(StringRedisTemplate redisTemplate, String cacheKey) {
        this.redisTemplate = redisTemplate;
        this.cacheKey = cacheKey;
    }

    @Override
    public Optional<CachedJwks> get() {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(cacheKey);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        String jwksJson = toString(values.get(FIELD_JWKS_JSON));
        String fetchedAtRaw = toString(values.get(FIELD_FETCHED_AT));
        String expiresAtRaw = toString(values.get(FIELD_EXPIRES_AT));
        if (jwksJson == null || fetchedAtRaw == null || expiresAtRaw == null) {
            return Optional.empty();
        }
        return Optional.of(new CachedJwks(
                jwksJson,
                Instant.ofEpochMilli(Long.parseLong(fetchedAtRaw)),
                Instant.ofEpochMilli(Long.parseLong(expiresAtRaw))
        ));
    }

    @Override
    public void put(CachedJwks jwks) {
        redisTemplate.opsForHash().put(cacheKey, FIELD_JWKS_JSON, jwks.jwksJson());
        redisTemplate.opsForHash().put(cacheKey, FIELD_FETCHED_AT, String.valueOf(jwks.fetchedAt().toEpochMilli()));
        redisTemplate.opsForHash().put(cacheKey, FIELD_EXPIRES_AT, String.valueOf(jwks.expiresAt().toEpochMilli()));
    }

    @Override
    public void evict() {
        redisTemplate.delete(cacheKey);
    }

    private String toString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
