package com.bbmovie.search.service.nats;

import com.example.common.dtos.events.SystemHealthEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Log4j2
public class HealthEventProducerImpl extends AbstractNatsJetStreamService implements HealthEventProducer {

    private final ObjectMapper objectMapper;

    @Autowired
    public HealthEventProducerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishHealthEvent(SystemHealthEvent event) {
        JetStream jetStream = getJetStream();
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
