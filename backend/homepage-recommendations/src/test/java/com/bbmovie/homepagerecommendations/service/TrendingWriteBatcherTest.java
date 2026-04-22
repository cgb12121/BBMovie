package com.bbmovie.homepagerecommendations.service;

import com.bbmovie.homepagerecommendations.config.HomepageRecommendationsProperties;
import com.bbmovie.homepagerecommendations.dto.PlaybackAnalyticsEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrendingWriteBatcherTest {

    private StringRedisTemplate redis;
    private CircuitBreaker circuitBreaker;
    private HomepageRecommendationsProperties properties;
    private TrendingWriteBatcher batcher;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        circuitBreaker = mock(CircuitBreaker.class);
        properties = new HomepageRecommendationsProperties();
        properties.getTrending().setZoneId("UTC");
        properties.getTrending().setDailyKeyPrefix("homepage:trending:day");
        properties.getTrending().setDailyKeyTtlDays(8);
        when(redis.getStringSerializer()).thenReturn(RedisSerializer.string());
        when(redis.executePipelined(any(RedisCallback.class))).thenReturn(List.of());
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(circuitBreaker).executeRunnable(any());

        batcher = new TrendingWriteBatcher(redis, properties, circuitBreaker);
    }

    @Test
    void recordFlushesImmediatelyWhenBatchDisabled() {
        properties.getTrending().getBatch().setEnabled(false);
        PlaybackAnalyticsEvent event = playbackEvent();

        batcher.record(event);

        verify(redis, times(1)).executePipelined(any(RedisCallback.class));
    }

    @Test
    void recordFlushesWhenBufferReachesMaxSize() {
        properties.getTrending().getBatch().setEnabled(true);
        properties.getTrending().getBatch().setMaxBufferSize(2);

        batcher.record(playbackEvent());
        verify(redis, never()).executePipelined(any(RedisCallback.class));

        batcher.record(playbackEvent());
        verify(redis, times(1)).executePipelined(any(RedisCallback.class));
    }

    @Test
    void scheduledFlushFlushesBufferedEvents() {
        properties.getTrending().getBatch().setEnabled(true);
        properties.getTrending().getBatch().setMaxBufferSize(100);
        batcher.record(playbackEvent());

        batcher.scheduledFlush();

        verify(redis, times(1)).executePipelined(any(RedisCallback.class));
    }

    private PlaybackAnalyticsEvent playbackEvent() {
        return new PlaybackAnalyticsEvent(
                UUID.randomUUID().toString(),
                "u1",
                UUID.randomUUID(),
                2,
                120.0,
                1710000000L,
                true,
                null
        );
    }
}

