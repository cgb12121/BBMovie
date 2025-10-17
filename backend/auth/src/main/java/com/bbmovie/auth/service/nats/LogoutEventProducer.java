package com.bbmovie.auth.service.nats;

import com.bbmovie.auth.dto.event.NatsConnectionEvent;
import io.nats.client.ConnectionListener;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Service
public class LogoutEventProducer {

    private final AtomicReference<JetStream> jetStreamRef = new AtomicReference<>();

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected, initializing JetStream context for LogoutEventProducer.");
            try {
                jetStreamRef.set(event.connection().jetStream());
            } catch (IOException e) {
                log.error("Failed to create JetStream context for LogoutEventProducer.", e);
            }
        }
    }

    public void send(String key) {
        JetStream jetStream = jetStreamRef.get();
        if (jetStream == null) {
            log.warn("NATS JetStream not available. Skipping logout event publication for key: {}", key);
            return;
        }
        try {
            byte[] data = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            jetStream.publish("auth.logout", data);
            log.info("Published logout event to auth.logout with key for cache: {}", key);
        } catch (Exception e) {
            log.error("Failed to publish logout event with key: {}", key, e);
        }
    }
}
