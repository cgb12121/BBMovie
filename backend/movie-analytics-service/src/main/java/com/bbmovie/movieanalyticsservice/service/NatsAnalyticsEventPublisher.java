package com.bbmovie.movieanalyticsservice.service;

import com.bbmovie.movieanalyticsservice.config.MovieAnalyticsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "movie.analytics.messaging.nats.enabled", havingValue = "true")
public class NatsAnalyticsEventPublisher implements AnalyticsEventPublisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final MovieAnalyticsProperties properties;

    @Override
    public void publishHeatmapRaw(HeatmapIngestEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            natsConnection.publish(
                    properties.getMessaging().getNats().getSubject(),
                    payload.getBytes(StandardCharsets.UTF_8)
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize heatmap event for NATS: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to publish heatmap event to NATS: {}", ex.getMessage());
        }
    }
}

