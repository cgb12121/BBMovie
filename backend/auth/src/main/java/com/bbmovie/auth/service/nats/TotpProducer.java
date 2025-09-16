package com.bbmovie.auth.service.nats;

import com.example.common.enums.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TotpProducer {

    private final JetStream jetStream;

    @Autowired
    public TotpProducer(Connection nats) throws java.io.IOException {
        this.jetStream = nats.jetStream();
    }

    public void sendNotification(String userId, NotificationType type, String destination, String message) {
        try {
            java.util.Map<String, Object> payload = java.util.Map.of(
                    "userId", userId,
                    "type", type != null ? type.name() : null,
                    "destination", destination,
                    "message", message
            );
            byte[] data = new ObjectMapper().writeValueAsBytes(payload);
            jetStream.publish("auth.otp", data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish OTP notification", e);
        }
    }
}