package com.bbmovie.movieanalyticsservice.controller.openapi;

import com.bbmovie.movieanalyticsservice.dto.HeatmapIngestRequest;
import com.bbmovie.movieanalyticsservice.dto.HeatmapIngestedResponse;
import com.bbmovie.movieanalyticsservice.dto.HeatmapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "Heatmap Analytics", description = "Movie playback heatmap APIs")
public interface HeatmapControllerOpenApi {
    @Operation(summary = "Ingest heatmap segment")
    HeatmapIngestedResponse ingest(@Valid @RequestBody HeatmapIngestRequest request);

    @Operation(summary = "Get movie heatmap")
    HeatmapResponse heatmap(@PathVariable UUID movieId, @RequestParam(required = false) Integer bucketSize);
}

