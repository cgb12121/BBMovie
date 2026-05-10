package com.bbmovie.movieanalyticsservice.service;

import com.bbmovie.movieanalyticsservice.config.MovieAnalyticsProperties;
import com.bbmovie.movieanalyticsservice.dto.HeatmapIngestRequest;
import com.bbmovie.movieanalyticsservice.dto.HeatmapIngestedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HeatmapAggregationService {

    private final StringRedisTemplate redis;
    private final MovieAnalyticsProperties properties;
    private final CompositeAnalyticsEventPublisher eventPublisher;
    private final Optional<ClickHouseHeatmapRepository> clickHouseHeatmapRepository;

    public HeatmapIngestedResponse ingest(HeatmapIngestRequest request) {
        int bucketSize = request.bucketSize() > 0
                ? request.bucketSize()
                : properties.getHeatmap().getBucketSizeDefault();
        Map<Integer, Integer> segmentCounts = toSegmentCounts(request);
        String redisKey = redisKey(request.movieId(), bucketSize);
        for (Map.Entry<Integer, Integer> entry : segmentCounts.entrySet()) {
            redis.opsForZSet().incrementScore(redisKey, Integer.toString(entry.getKey()), entry.getValue());
        }
        HeatmapIngestEvent event = new HeatmapIngestEvent(request.movieId(), bucketSize, segmentCounts, Instant.now());
        eventPublisher.publishHeatmapRaw(event);
        clickHouseHeatmapRepository.ifPresent(repo -> repo.appendEvent(event));

        return new HeatmapIngestedResponse(request.movieId(), bucketSize, request.segments().size());
    }

    private Map<Integer, Integer> toSegmentCounts(HeatmapIngestRequest request) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Integer segment : request.segments()) {
            if (segment == null || segment < 0) {
                continue;
            }
            counts.merge(segment, 1, (a, b) -> a + b);
        }
        return counts;
    }

    public String redisKey(UUID movieId, int bucketSize) {
        return properties.getHeatmap().getRedisPrefix() + ":" + movieId + ":bucket:" + bucketSize;
    }
}

