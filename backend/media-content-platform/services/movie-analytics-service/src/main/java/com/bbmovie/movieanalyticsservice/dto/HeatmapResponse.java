package com.bbmovie.movieanalyticsservice.dto;

import java.util.List;
import java.util.UUID;

public record HeatmapResponse(
        UUID movieId,
        int bucketSize,
        List<Double> data,
        Peak peak
) {
    public record Peak(int segment, String timestamp, String label) {
    }
}

