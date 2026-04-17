package com.bbmovie.auth.service.nats;

import com.bbmovie.auth.dto.request.RegisterRequest;
import com.bbmovie.auth.entity.User;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Map;

@Log4j2
@Service
public class UserEventProducer extends AbstractNatsJetStreamService {

    public void publishUserRegistered(User user, String referredByCode) {
        Map<String, Object> event = Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "referralCode", user.getReferralCode(),
                "referredBy", referredByCode != null ? referredByCode : ""
        );
        publish("auth.user.registered", event);
    }

    private void publish(String subject, Map<String, Object> event) {
        JetStream jetStream = getJetStream();
        if (jetStream == null) {
            log.warn("NATS JetStream not available. Skipping event publication to subject: {}", subject);
            return;
        }
        try {
            byte[] data = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(event);
            jetStream.publish(subject, data);
            log.info("Sent event to subject: {} for user: {}", subject, event.get("email"));
        } catch (Exception e) {
            log.error("Failed to send event to subject {}: {}", subject, e.getMessage());
        }
    }
}
