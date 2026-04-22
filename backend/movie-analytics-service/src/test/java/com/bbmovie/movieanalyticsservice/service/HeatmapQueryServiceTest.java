package com.bbmovie.movieanalyticsservice.service;

import com.bbmovie.movieanalyticsservice.config.MovieAnalyticsProperties;
import com.bbmovie.movieanalyticsservice.dto.HeatmapResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeatmapQueryServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void readNormalizesDataAndReturnsPeak() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zSetOperations);

        UUID movieId = UUID.randomUUID();
        String key = "movie:heatmap:" + movieId + ":bucket:10";
        Set<ZSetOperations.TypedTuple<String>> tuples = Set.of(
                new DefaultTypedTuple<>("0", 10.0),
                new DefaultTypedTuple<>("1", 30.0),
                new DefaultTypedTuple<>("2", 20.0)
        );
        when(zSetOperations.rangeWithScores(eq(key), eq(0L), anyLong())).thenReturn(tuples);

        MovieAnalyticsProperties properties = new MovieAnalyticsProperties();
        HeatmapQueryService service = new HeatmapQueryService(redis, properties, Optional.empty());

        HeatmapResponse response = service.read(movieId, 10);

        assertEquals(movieId, response.movieId());
        assertEquals(10, response.bucketSize());
        assertEquals(3, response.data().size());
        assertNotNull(response.peak());
        assertEquals(1, response.peak().segment());
        assertEquals("00:00:10", response.peak().timestamp());
    }
}

