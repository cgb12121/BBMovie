package com.bbmovie.movieanalyticsservice.service;

import com.bbmovie.movieanalyticsservice.config.MovieAnalyticsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "movie.analytics.messaging.kafka.enabled", 
    havingValue = "true"
)
public class KafkaAnalyticsEventPublisher implements AnalyticsEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MovieAnalyticsProperties properties;

    @Override
    public void publishHeatmapRaw(HeatmapIngestEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(properties.getMessaging().getKafka().getTopic(), event.movieId().toString(), payload);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize heatmap event for Kafka: {}", ex.getMessage());
        }
    }
}

