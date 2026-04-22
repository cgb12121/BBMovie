package com.bbmovie.movieanalyticsservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record HeatmapIngestRequest(
        @NotNull UUID movieId,
        @Positive int bucketSize,
        @NotEmpty List<Integer> segments
) {
}

