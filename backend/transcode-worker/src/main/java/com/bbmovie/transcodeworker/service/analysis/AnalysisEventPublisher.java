package com.bbmovie.transcodeworker.service.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisEventPublisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    @Value("${app.analysis.events.subject:media.analysis.events}")
    private String analysisSubject;

    public void publish(String uploadId, String eventType, Map<String, Object> payload) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("uploadId", uploadId);
            body.put("eventType", eventType);
            body.put("createdAt", Instant.now().toString());
            body.put("payload", payload);
            String json = objectMapper.writeValueAsString(body);
            natsConnection.publish(analysisSubject, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("Failed to publish analysis event {}", eventType, e);
        }
    }
}
