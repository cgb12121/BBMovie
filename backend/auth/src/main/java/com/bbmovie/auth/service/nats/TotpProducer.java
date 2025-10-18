package com.bbmovie.auth.service.nats;

import com.bbmovie.auth.dto.event.NatsConnectionEvent;
import com.example.common.enums.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.ConnectionListener;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Service
public class TotpProducer {

    private final AtomicReference<JetStream> jetStreamRef = new AtomicReference<>();

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected, initializing JetStream context for TotpProducer.");
            try {
                jetStreamRef.set(event.connection().jetStream());
            } catch (IOException e) {
                log.error("Failed to create JetStream context for TotpProducer.", e);
            }
        }
    }

    public void sendNotification(String userId, NotificationType type, String destination, String message) {
        JetStream jetStream = jetStreamRef.get();
        if (jetStream == null) {
            log.warn("NATS JetStream not available. Skipping TOTP notification for user: {}", userId);
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "userId", userId,
                    "type", type != null ? type.name() : "UNKNOWN",
                    "destination", destination,
                    "message", message
            );
            byte[] data = new ObjectMapper().writeValueAsBytes(payload);
            jetStream.publish("auth.otp", data);
        } catch (Exception e) {
            // Do not throw exception to avoid breaking business logic flow
            log.error("Failed to publish OTP notification for user: {}", userId, e);
        }
    }
}