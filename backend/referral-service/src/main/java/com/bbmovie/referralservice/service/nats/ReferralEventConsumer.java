package com.bbmovie.referralservice.service.nats;

import com.bbmovie.referralservice.dto.event.NatsConnectionEvent;
import com.bbmovie.referralservice.service.ReferralService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Dispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReferralEventConsumer {

    private final ObjectMapper objectMapper;
    private final ReferralService referralService;

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected, subscribing to referral events...");
            setupSubscriptions(event.connection());
        }
    }

    private void setupSubscriptions(Connection nats) {
        Dispatcher dispatcher = nats.createDispatcher(msg -> {
            try {
                String subject = msg.getSubject();

                @SuppressWarnings("all")
                Map<String, Object> event = objectMapper.readValue(msg.getData(), Map.class);

                log.debug("Received event on subject: {}", subject);

                if ("auth.user.registered".equals(subject)) {
                    String userIdStr = (String) event.get("userId");
                    String email = (String) event.get("email");
                    String referralCode = (String) event.get("referralCode");
                    String referredBy = (String) event.get("referredBy");

                    UUID userId = UUID.fromString(userIdStr);
                    // 1. Save user info locally
                    referralService.saveUser(userId, email, referralCode);
                    // 2. Process referral link
                    referralService.handleUserRegistered(userId, referredBy);
                } else if ("payments.subscription.success".equals(subject)) {
                    String userIdStr = (String) event.get("userId");
                    referralService.handleSubscriptionSuccess(userIdStr);
                }
            } catch (Exception e) {
                log.error("Error processing event: {}", e.getMessage());
            }
        });

        dispatcher.subscribe("auth.user.registered");
        dispatcher.subscribe("payments.subscription.success");
        log.debug("Subscribed to auth.user.registered and payments.subscription.success");
    }
}
