package com.bbmovie.email.service.nats;

import com.bbmovie.email.service.email.EmailService;
import com.bbmovie.email.service.email.EmailServiceFactory;
import com.example.common.dtos.nats.SubscriptionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class SubscriptionEventConsumer {

    @Autowired
    public SubscriptionEventConsumer(Connection nats, ObjectMapper objectMapper, EmailServiceFactory emailServiceFactory) throws Exception {
        JetStream js = nats.jetStream();

        // Durable Pull consumer for subscription events stream
        JetStreamSubscription sub = js.subscribe(
                "payments.subscription.*",
                PullSubscribeOptions.builder().durable("email-subscription-service").build()
        );

        Dispatcher dispatcher = nats.createDispatcher(msg -> {
            try {
                SubscriptionEvent event = objectMapper.readValue(msg.getData(), SubscriptionEvent.class);
                handle(event, emailServiceFactory);
                msg.ack();
            } catch (Exception e) {
                log.error("Failed to process subscription event", e);
                msg.nak();
            }
        });

        dispatcher.subscribe("payments.subscription.*");
    }

    private void handle(SubscriptionEvent event, EmailServiceFactory factory) {
        EmailService email = factory.getRotationStrategies().getFirst();
        switch (event.eventType()) {
            case "SUBSCRIBED" -> {
                // You may add a dedicated method in EmailService if needed
                log.info("User {} subscribed to {}", event.userEmail(), event.planName());
            }
            case "RENEWAL_UPCOMING" -> {
                log.info("Upcoming renewal for user {} on plan {}", event.userEmail(), event.planName());
            }
            case "CANCELLED" -> {
                log.info("Subscription cancelled for user {} on plan {}", event.userEmail(), event.planName());
            }
            default -> log.warn("Unknown subscription event: {}", event.eventType());
        }
    }
}


