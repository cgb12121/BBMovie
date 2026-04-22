package com.bbmovie.movieanalyticsservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CompositeAnalyticsEventPublisher implements AnalyticsEventPublisher {

    private final List<AnalyticsEventPublisher> delegates;

    @Override
    public void publishHeatmapRaw(HeatmapIngestEvent event) {
        for (AnalyticsEventPublisher delegate : delegates) {
            if (delegate == this) {
                continue;
            }
            delegate.publishHeatmapRaw(event);
        }
    }
}

