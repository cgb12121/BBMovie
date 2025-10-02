package com.bbmovie.email.service.nats;

import com.bbmovie.email.config.NatsConfig;
import com.bbmovie.email.dto.event.NatsConnectionEvent;
import com.bbmovie.email.service.email.EmailService;
import com.bbmovie.email.service.email.EmailServiceFactory;
import com.example.common.dtos.nats.SubscriptionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Log4j2
@Service
public class SubscriptionEventConsumer {

    private final Semaphore limit = new Semaphore(100);
    private final ExecutorService emailExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final Connection nats;
    private final ObjectMapper objectMapper;
    private final EmailServiceFactory emailServiceFactory;

    @Autowired
    public SubscriptionEventConsumer(NatsConfig.NatsConnectionFactory natsConnectionFactory, ObjectMapper objectMapper, EmailServiceFactory emailServiceFactory) {
        this.nats = natsConnectionFactory.getConnection();
        this.objectMapper = objectMapper;
        this.emailServiceFactory = emailServiceFactory;
    }

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected/reconnected, (re)subscribing…");
            setupSubscriptionServiceEventSubscriptions();
        }
    }

    public void setupSubscriptionServiceEventSubscriptions() {
        Dispatcher dispatcher = this.nats.createDispatcher(msg -> {
            try {
                String subject = msg.getSubject();
                SubscriptionEvent event = objectMapper.readValue(msg.getData(), SubscriptionEvent.class);
                emailExecutor.submit(() -> {
                    try {
                        limit.acquire();
                        handle(subject, event, emailServiceFactory);
                    } catch (Exception e) {
                        log.error("Error while processing email for subject {}", subject, e);
                    } finally {
                        limit.release();
                    }
                });
            } catch (Exception e) {
                log.error("Failed to process subscription event", e);
                msg.nak();
            }
        });

        dispatcher.subscribe("payments.subscription.*");
    }

    private void handle(String subject, SubscriptionEvent event, EmailServiceFactory factory) {
        EmailService email = factory.getRotationStrategies().getFirst();
        switch (subject) {
            case "payments.subscription.subscribe" -> {
                email.send();
                log.info("User {} subscribed to {}", event.userEmail(), event.planName());
            }
            case "payments.subscription.renewal.upcoming" -> {
                email.send();
                log.info("Upcoming renewal for user {} on plan {}", event.userEmail(), event.planName());
            }
            case "payments.subscription.cancelled" -> {
                email.send();
                log.info("Subscription cancelled for user {} on plan {}", event.userEmail(), event.planName());
            }
            default -> log.error("Unknown subscription event: {}", subject);
        }
    }
}


