package com.bbmovie.movieanalyticsservice.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record HeatmapIngestEvent(
        UUID movieId,
        int bucketSize,
        Map<Integer, Integer> segmentCounts,
        Instant occurredAt
) {
}

