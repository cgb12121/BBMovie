package com.bbmovie.homepagerecommendations.service;

import com.bbmovie.homepagerecommendations.config.HomepageRecommendationsProperties;
import com.bbmovie.homepagerecommendations.dto.PlaybackAnalyticsEvent;
import com.bbmovie.homepagerecommendations.dto.TrendingResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.connection.zset.Weights;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrendingAggregationServiceTest {

    private StringRedisTemplate redis;
    private ZSetOperations<String, String> zSetOperations;
    private TrendingWriteBatcher trendingWriteBatcher;
    private CircuitBreaker circuitBreaker;
    private HomepageRecommendationsProperties properties;
    private TrendingAggregationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        zSetOperations = mock(ZSetOperations.class);
        trendingWriteBatcher = mock(TrendingWriteBatcher.class);
        circuitBreaker = mock(CircuitBreaker.class);
        properties = new HomepageRecommendationsProperties();
        properties.getTrending().setDailyKeyPrefix("homepage:trending:day");
        properties.getTrending().setZoneId("UTC");
        properties.getTrending().setUnionTempKeyPrefix("homepage:trending:union:tmp:");
        when(redis.opsForZSet()).thenReturn(zSetOperations);

        doAnswer(invocation -> {
            Supplier<TrendingResponse> supplier = invocation.getArgument(0);
            return supplier.get();
        }).when(circuitBreaker).executeSupplier(any());

        service = new TrendingAggregationService(redis, properties, trendingWriteBatcher, circuitBreaker);
    }

    @Test
    void incrementFromPlaybackEventDelegatesToBatcher() {
        PlaybackAnalyticsEvent event = new PlaybackAnalyticsEvent(
                "evt-1",
                "u1",
                UUID.randomUUID(),
                1,
                10.0,
                1710000000L,
                false,
                null
        );

        service.incrementFromPlaybackEvent(event);

        verify(trendingWriteBatcher).record(event);
    }

    @Test
    void topTrendingReturnsEmptyWhenLimitInvalid() {
        TrendingResponse response = service.topTrending(0);

        assertTrue(response.items().isEmpty());
        verify(circuitBreaker, never()).executeSupplier(any());
    }

    @Test
    void topTrendingWithSingleDayReadsCurrentDailyKey() {
        properties.getTrending().setMergeWindowDays(1);
        UUID movieId = UUID.randomUUID();
        Set<ZSetOperations.TypedTuple<String>> tuples =
                Set.of(new DefaultTypedTuple<>(movieId.toString(), 9.5));

        String expectedKey = "homepage:trending:day:" + LocalDate.now(ZoneId.of("UTC"));
        when(zSetOperations.reverseRangeWithScores(eq(expectedKey), eq(0L), eq(2L))).thenReturn(tuples);

        TrendingResponse response = service.topTrending(3);

        assertEquals(1, response.items().size());
        assertEquals(movieId, response.items().getFirst().movieId());
        assertEquals(9.5, response.items().getFirst().score());
        verify(zSetOperations, never()).unionAndStore(anyString(), any(), anyString(), any(Aggregate.class), any(Weights.class));
        verify(redis, never()).delete(anyString());
    }

    @Test
    void topTrendingWithWindowUnionStoresAndDeletesTempKey() {
        properties.getTrending().setMergeWindowDays(3);
        UUID movieId = UUID.randomUUID();
        Set<ZSetOperations.TypedTuple<String>> tuples =
                Set.of(new DefaultTypedTuple<>(movieId.toString(), 11.0));

        ArgumentCaptor<String> tempKeyCaptor = ArgumentCaptor.forClass(String.class);
        when(zSetOperations.unionAndStore(anyString(), any(), tempKeyCaptor.capture(), eq(Aggregate.SUM), any(Weights.class)))
                .thenReturn(1L);
        when(zSetOperations.reverseRangeWithScores(anyString(), eq(0L), eq(1L))).thenReturn(tuples);

        TrendingResponse response = service.topTrending(2);

        assertEquals(1, response.items().size());
        assertEquals(movieId, response.items().getFirst().movieId());
        String tempKey = tempKeyCaptor.getValue();
        verify(zSetOperations, times(1))
                .reverseRangeWithScores(eq(tempKey), eq(0L), eq(1L));
        verify(redis, times(1)).delete(eq(tempKey));
    }
}

