package com.bbmovie.personalizationrecommendation.service;

import com.bbmovie.personalizationrecommendation.config.PersonalizationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RedisUserFeatureRepository implements UserFeatureRepository {

    private final StringRedisTemplate redis;
    private final PersonalizationProperties properties;

    @Override
    public Map<String, Double> readGenreAffinity(UUID userId) {
        String key = properties.getRedis().getUserGenreAffinityPrefix() + userId;
        Map<Object, Object> map = redis.opsForHash().entries(key);
        Map<String, Double> out = new HashMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            try {
                out.put(entry.getKey().toString(), Double.parseDouble(entry.getValue().toString()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    @Override
    public Set<UUID> readSeenMovieIds(UUID userId) {
        String key = properties.getRedis().getUserSeenMoviesPrefix() + userId;
        Set<String> members = redis.opsForSet().members(key);
        Set<UUID> out = new HashSet<>();
        if (members == null) {
            return out;
        }
        for (String member : members) {
            try {
                out.add(UUID.fromString(member));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }
}

