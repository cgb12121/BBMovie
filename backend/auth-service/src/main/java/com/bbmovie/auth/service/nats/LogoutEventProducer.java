package com.bbmovie.auth.service.nats;

import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Log4j2
@Service
public class LogoutEventProducer extends AbstractNatsJetStreamService {

    public void send(String key) {
        JetStream jetStream = getJetStream();
        if (jetStream == null) {
            log.warn("NATS JetStream not available. Skipping logout event publication for key: {}", key);
            return;
        }
        try {
            byte[] data = key.getBytes(StandardCharsets.UTF_8);
            jetStream.publish("auth.logout", data);
            log.info("Published logout event to auth.logout with key for cache: {}", key);
        } catch (Exception e) {
            log.error("Failed to publish logout event with key: {}", key, e);
        }
    }
}
