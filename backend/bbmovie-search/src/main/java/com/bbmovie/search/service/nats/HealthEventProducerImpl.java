package com.bbmovie.search.service.nats;

import com.bbmovie.search.dto.event.NatsConnectionEvent;
import com.example.common.dtos.events.SystemHealthEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.ConnectionListener;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Log4j2
public class HealthEventProducerImpl implements HealthEventProducer {

    private final AtomicReference<JetStream> jetStreamRef = new AtomicReference<>();
    private final ObjectMapper objectMapper;

    @Autowired
    public HealthEventProducerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

    @Override
    public void publishHealthEvent(SystemHealthEvent event) {
        JetStream jetStream = jetStreamRef.get();
        if (jetStream == null) {
            log.warn("NATS JetStream is not available. Skipping health event publication for service: {}", event.getService());
            return;
        }

        try {
            byte[] data = objectMapper.writeValueAsBytes(event);
            PublishAck ack = jetStream.publish("system.health", data);
            log.info("Published health event to subject [system.health] with seq: {}", ack.getSeqno());
        } catch (IOException e) {
            log.error("Failed to serialize health event: {}", e.getMessage());
        } catch (JetStreamApiException e) {
            log.error("Failed to publish health event: {}", e.getMessage());
        }
    }
}
