package com.bbmovie.movieanalyticsservice.dto;

import java.util.UUID;

public record HeatmapIngestedResponse(
        UUID movieId,
        int bucketSize,
        int acceptedSegments
) {
}

