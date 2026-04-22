package com.bbmovie.homepagerecommendations.service;

import com.bbmovie.homepagerecommendations.config.HomepageRecommendationsProperties;
import com.bbmovie.homepagerecommendations.dto.PlaybackAnalyticsEvent;
import com.bbmovie.homepagerecommendations.dto.TrendingEntry;
import com.bbmovie.homepagerecommendations.dto.TrendingResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.connection.zset.Weights;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendingAggregationService {

    private final StringRedisTemplate redis;
    private final HomepageRecommendationsProperties properties;
    private final TrendingWriteBatcher trendingWriteBatcher;
    private final CircuitBreaker redisTrendingCircuitBreaker;

    public void incrementFromPlaybackEvent(PlaybackAnalyticsEvent event) {
        trendingWriteBatcher.record(event);
    }

    public TrendingResponse topTrending(int limit) {
        if (limit <= 0) {
            return new TrendingResponse(List.of());
        }
        try {
            return redisTrendingCircuitBreaker.executeSupplier(() -> readTopTrending(limit));
        } catch (CallNotPermittedException ex) {
            log.warn("Redis circuit open; returning empty trending");
            return new TrendingResponse(List.of());
        } catch (DataAccessException ex) {
            log.warn("Redis trending read failed: {}", ex.getMessage());
            return new TrendingResponse(List.of());
        }
    }

    private TrendingResponse readTopTrending(int limit) {
        int window = Math.max(1, properties.getTrending().getMergeWindowDays());
        LocalDate today = LocalDate.now(TrendingEventSupport.zone(properties));
        List<String> dayKeys = new ArrayList<>(window);
        for (int i = 0; i < window; i++) {
            dayKeys.add(TrendingEventSupport.dailyZsetKey(today.minusDays(i), properties));
        }

        Set<ZSetOperations.TypedTuple<String>> tuples;
        if (dayKeys.size() == 1) {
            tuples = redis.opsForZSet().reverseRangeWithScores(dayKeys.getFirst(), 0, (long) limit - 1);
        } else {
            double gravity = properties.getTrending().getGravity();
            double[] weights = new double[dayKeys.size()];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = 1.0 / Math.pow(i + 2.0, gravity);
            }
            String first = dayKeys.getFirst();
            List<String> rest = dayKeys.subList(1, dayKeys.size());
            String tempKey = properties.getTrending().getUnionTempKeyPrefix() + UUID.randomUUID();
            try {
                redis.opsForZSet().unionAndStore(first, rest, tempKey, Aggregate.SUM, Weights.of(weights));
                tuples = redis.opsForZSet().reverseRangeWithScores(tempKey, 0, (long) limit - 1);
            } finally {
                redis.delete(tempKey);
            }
        }

        if (tuples == null || tuples.isEmpty()) {
            return new TrendingResponse(List.of());
        }
        List<TrendingEntry> items = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            try {
                items.add(new TrendingEntry(UUID.fromString(tuple.getValue()), tuple.getScore()));
            } catch (IllegalArgumentException ex) {
                log.warn("Skip invalid movie id in trending ZSET: {}", tuple.getValue());
            }
        }
        return new TrendingResponse(items);
    }
}
