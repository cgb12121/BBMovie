package com.bbmovie.movieanalyticsservice.service;

import com.bbmovie.movieanalyticsservice.config.MovieAnalyticsProperties;
import com.bbmovie.movieanalyticsservice.dto.HeatmapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HeatmapQueryService {

    private final StringRedisTemplate redis;
    private final MovieAnalyticsProperties properties;
    private final Optional<ClickHouseHeatmapRepository> clickHouseHeatmapRepository;

    public HeatmapResponse read(UUID movieId, Integer requestedBucketSize) {
        int bucketSize = requestedBucketSize != null && requestedBucketSize > 0
                ? requestedBucketSize
                : properties.getHeatmap().getBucketSizeDefault();
        List<BucketPoint> points = readFromRedis(movieId, bucketSize);
        if (points.isEmpty()) {
            points = readFromClickHouse(movieId, bucketSize);
        }
        return toResponse(movieId, bucketSize, points);
    }

    private List<BucketPoint> readFromRedis(UUID movieId, int bucketSize) {
        String redisKey = properties.getHeatmap().getRedisPrefix() + ":" + movieId + ":bucket:" + bucketSize;
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet().rangeWithScores(
                redisKey,
                0,
                properties.getHeatmap().getMaxBuckets() - 1L
        );
        List<BucketPoint> points = new ArrayList<>();
        if (tuples == null) {
            return points;
        }
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            try {
                points.add(new BucketPoint(Integer.parseInt(tuple.getValue()), tuple.getScore()));
            } catch (NumberFormatException ignored) {
            }
        }
        points.sort(Comparator.comparingInt(BucketPoint::bucket));
        return points;
    }

    private List<BucketPoint> readFromClickHouse(UUID movieId, int bucketSize) {
        List<BucketPoint> points = new ArrayList<>();
        clickHouseHeatmapRepository.ifPresent(repo -> {
            repo.loadAggregated(movieId, bucketSize, properties.getHeatmap().getMaxBuckets())
                    .forEach((bucket, count) -> points.add(new BucketPoint(bucket, count.doubleValue())));
        });
        points.sort(Comparator.comparingInt(BucketPoint::bucket));
        return points;
    }

    private HeatmapResponse toResponse(UUID movieId, int bucketSize, List<BucketPoint> points) {
        if (points.isEmpty()) {
            return new HeatmapResponse(movieId, bucketSize, List.of(), null);
        }
        double min = points.stream().mapToDouble(BucketPoint::score).min().orElse(0.0);
        double max = points.stream().mapToDouble(BucketPoint::score).max().orElse(0.0);
        double range = max - min;

        List<Double> data = new ArrayList<>(points.size());
        BucketPoint peak = points.getFirst();
        for (BucketPoint p : points) {
            if (p.score() > peak.score()) {
                peak = p;
            }
            if (range <= 0.000001d) {
                data.add(1.0);
            } else {
                data.add((p.score() - min) / range);
            }
        }
        String peakTimestamp = formatSeconds(peak.bucket() * bucketSize);
        HeatmapResponse.Peak peakDto = new HeatmapResponse.Peak(peak.bucket(), peakTimestamp, "Most Replayed");
        return new HeatmapResponse(movieId, bucketSize, data, peakDto);
    }

    private String formatSeconds(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private record BucketPoint(int bucket, double score) {
    }
}

