package com.bbmovie.homepagerecommendations.service;

import com.bbmovie.homepagerecommendations.config.HomepageRecommendationsProperties;
import com.bbmovie.homepagerecommendations.dto.PlaybackAnalyticsEvent;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrendingWriteBatcher {

    private final StringRedisTemplate redis;
    private final HomepageRecommendationsProperties properties;
    private final CircuitBreaker redisTrendingCircuitBreaker;

    private final Object flushLock = new Object();
    private final ConcurrentHashMap<String, Double> buffer = new ConcurrentHashMap<>();

    public void record(PlaybackAnalyticsEvent event) {
        if (event == null || event.movieId() == null) {
            return;
        }
        HomepageRecommendationsProperties.Trending.Batch batch = properties.getTrending().getBatch();
        double delta = TrendingEventSupport.deltaFor(event, properties);
        if (delta == 0.0) {
            return;
        }
        LocalDate day = TrendingEventSupport.dayForEvent(event, TrendingEventSupport.zone(properties));
        String key = TrendingEventSupport.bufferCompositeKey(day, event.movieId());
        if (!batch.isEnabled()) {
            Map<String, Double> one = Map.of(key, delta);
            flushSnapshot(one);
            return;
        }
        buffer.merge(key, delta, (a, b) -> a + b);
        if (buffer.size() >= batch.getMaxBufferSize()) {
            flush();
        }
    }

    @Scheduled(fixedDelayString = "${homepage.recommendations.trending.batch.flush-interval-ms:5000}")
    public void scheduledFlush() {
        flush();
    }

    public void flush() {
        Map<String, Double> snapshot;
        synchronized (flushLock) {
            if (buffer.isEmpty()) {
                return;
            }
            snapshot = new HashMap<>(buffer);
            buffer.clear();
        }
        flushSnapshot(snapshot);
    }

    private void flushSnapshot(Map<String, Double> snapshot) {
        if (snapshot.isEmpty()) {
            return;
        }
        try {
            redisTrendingCircuitBreaker.executeRunnable(() -> writePipeline(snapshot));
        } catch (CallNotPermittedException ex) {
            log.warn("Redis circuit open; re-queuing {} trending deltas", snapshot.size());
            requeue(snapshot);
        } catch (DataAccessException ex) {
            log.warn("Redis trending write failed: {}", ex.getMessage());
            requeue(snapshot);
        }
    }

    private void requeue(Map<String, Double> snapshot) {
        synchronized (flushLock) {
            snapshot.forEach((k, v) -> buffer.merge(k, v, (a, b) -> a + b));
        }
    }

    private void writePipeline(Map<String, Double> snapshot) {
        long ttlSeconds = (long) properties.getTrending().getDailyKeyTtlDays() * 24L * 3600L;
        Set<String> touchedDayPrefixes = new HashSet<>();
        redis.executePipelined(new SessionCallback<>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                RedisOperations<String, String> stringOps = castToStringRedisOperations(operations);
                for (Map.Entry<String, Double> e : snapshot.entrySet()) {
                    String composite = e.getKey();
                    int sep = composite.indexOf('|');
                    if (sep < 1 || sep >= composite.length() - 1) {
                        continue;
                    }
                    LocalDate day = LocalDate.parse(composite.substring(0, sep));
                    String member = composite.substring(sep + 1);
                    String zkey = TrendingEventSupport.dailyZsetKey(day, properties);
                    touchedDayPrefixes.add(zkey);
                    stringOps.opsForZSet().incrementScore(zkey, member, e.getValue());
                }
                for (String zkey : touchedDayPrefixes) {
                    stringOps.expire(zkey, Duration.ofSeconds(ttlSeconds));
                }
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <K, V> RedisOperations<String, String> castToStringRedisOperations(RedisOperations<K, V> operations) {
        return (RedisOperations<String, String>) operations;
    }

    @PreDestroy
    void flushOnShutdown() {
        flush();
    }
}
