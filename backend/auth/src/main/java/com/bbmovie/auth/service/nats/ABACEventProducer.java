package com.bbmovie.auth.service.nats;

import com.bbmovie.auth.dto.event.NatsConnectionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.ConnectionListener;
import io.nats.client.JetStreamApiException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import io.nats.client.JetStream;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Service
public class ABACEventProducer {

    private final AtomicReference<JetStream> jetStreamRef = new AtomicReference<>();
    private final ObjectMapper objectMapper;

    @Autowired
    public ABACEventProducer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void send(String key) {
        JetStream jetStream = jetStreamRef.get();
        try {
            byte[] data = objectMapper.writeValueAsBytes(key);
            jetStream.publish("auth.abac", data);
        } catch (IOException e) {
            log.error("Failed to serialize health event: {}", e.getMessage());
        } catch (JetStreamApiException e) {
            log.error("Failed to publish health event: {}", e.getMessage());
        }
    }

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected, initializing JetStream context for HealthEventProducer.");
            try {
                jetStreamRef.set(event.connection().jetStream());
            } catch (IOException e) {
                log.error("Failed to create JetStream context after NATS connection was established.", e);
            }
        }
    }
}
