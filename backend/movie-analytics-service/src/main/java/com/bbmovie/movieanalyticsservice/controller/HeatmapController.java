package com.bbmovie.movieanalyticsservice.controller;

import com.bbmovie.movieanalyticsservice.controller.openapi.HeatmapControllerOpenApi;
import com.bbmovie.movieanalyticsservice.dto.HeatmapIngestRequest;
import com.bbmovie.movieanalyticsservice.dto.HeatmapIngestedResponse;
import com.bbmovie.movieanalyticsservice.dto.HeatmapResponse;
import com.bbmovie.movieanalyticsservice.service.HeatmapAggregationService;
import com.bbmovie.movieanalyticsservice.service.HeatmapQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics/v1/movies")
public class HeatmapController implements HeatmapControllerOpenApi {

    private final HeatmapAggregationService heatmapAggregationService;
    private final HeatmapQueryService heatmapQueryService;

    @PostMapping("/heatmap/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public HeatmapIngestedResponse ingest(@Valid @RequestBody HeatmapIngestRequest request) {
        return heatmapAggregationService.ingest(request);
    }

    @GetMapping("/{movieId}/heatmap")
    public HeatmapResponse heatmap(
            @PathVariable UUID movieId,
            @RequestParam(required = false) Integer bucketSize
    ) {
        return heatmapQueryService.read(movieId, bucketSize);
    }
}

