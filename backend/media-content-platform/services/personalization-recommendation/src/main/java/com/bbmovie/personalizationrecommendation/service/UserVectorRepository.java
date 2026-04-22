package com.bbmovie.personalizationrecommendation.service;

import com.bbmovie.personalizationrecommendation.config.PersonalizationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserVectorRepository {

    private final StringRedisTemplate redis;
    private final PersonalizationProperties properties;

    public List<Double> readUserProfileVector(UUID userId) {
        String value = redis.opsForValue().get(properties.getQdrant().getVectorKeyPrefix() + userId);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] chunks = value.split(",");
        List<Double> vector = new ArrayList<>(chunks.length);
        for (String chunk : chunks) {
            try {
                vector.add(Double.parseDouble(chunk.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return vector;
    }
}

