package com.bbmovie.movieanalyticsservice.service;

public interface AnalyticsEventPublisher {
    void publishHeatmapRaw(HeatmapIngestEvent event);
}

