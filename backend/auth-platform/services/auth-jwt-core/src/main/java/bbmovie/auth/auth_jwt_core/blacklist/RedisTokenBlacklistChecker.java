package bbmovie.auth.auth_jwt_core.blacklist;

import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisTokenBlacklistChecker implements TokenBlacklistChecker {
    private final StringRedisTemplate redisTemplate;
    private final String sidPrefix;
    private final String jtiPrefix;

    public RedisTokenBlacklistChecker(StringRedisTemplate redisTemplate, String sidPrefix, String jtiPrefix) {
        this.redisTemplate = redisTemplate;
        this.sidPrefix = sidPrefix;
        this.jtiPrefix = jtiPrefix;
    }

    @Override
    public boolean isRevoked(String sid, String jti) {
        boolean sidRevoked = sid != null && !sid.isBlank()
                && Boolean.TRUE.equals(redisTemplate.hasKey(sidPrefix + sid));
        boolean jtiRevoked = jti != null && !jti.isBlank()
                && Boolean.TRUE.equals(redisTemplate.hasKey(jtiPrefix + jti));
        return sidRevoked || jtiRevoked;
    }
}
