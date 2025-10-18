package com.bbmovie.auth.service.nats;

import com.example.common.enums.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Map;

@Log4j2
@Service
public class TotpProducer extends AbstractNatsJetStreamService {

    public void sendNotification(String userId, NotificationType type, String destination, String message) {
        JetStream jetStream = getJetStream();
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