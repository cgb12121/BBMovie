package com.bbmovie.mediaservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieEventPublisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    @Value("${nats.movie.published.subject:movie.published}")
    private String moviePublishedSubject;

    @Value("${nats.movie.deleted.subject:movie.deleted}")
    private String movieDeletedSubject;

    public void publishMoviePublishedEvent(UUID movieId, String title, String filePath) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("movieId", movieId != null ? movieId.toString() : null);
            payloadMap.put("title", title);
            payloadMap.put("filePath", filePath);
            payloadMap.put("timestamp", System.currentTimeMillis());
            
            String eventPayload = objectMapper.writeValueAsString(payloadMap);
            natsConnection.publish(moviePublishedSubject, eventPayload.getBytes());
            log.info("Published movie published event for movie: {} to subject: {}", movieId, moviePublishedSubject);
        } catch (Exception e) {
            log.error("Failed to publish movie published event for movie: {}", movieId, e);
        }
    }

    public void publishMovieDeletedEvent(UUID movieId, String title) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("movieId", movieId != null ? movieId.toString() : null);
            payloadMap.put("title", title);
            payloadMap.put("timestamp", System.currentTimeMillis());
            
            String eventPayload = objectMapper.writeValueAsString(payloadMap);
            natsConnection.publish(movieDeletedSubject, eventPayload.getBytes());
            log.info("Published movie deleted event for movie: {} to subject: {}", movieId, movieDeletedSubject);
        } catch (Exception e) {
            log.error("Failed to publish movie deleted event for movie: {}", movieId, e);
        }
    }
}
