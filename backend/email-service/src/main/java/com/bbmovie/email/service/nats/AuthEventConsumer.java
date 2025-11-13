package com.bbmovie.email.service.nats;

import com.bbmovie.email.dto.event.NatsConnectionEvent;
import com.bbmovie.email.exception.CustomEmailException;
import com.bbmovie.email.service.email.EmailService;
import com.bbmovie.email.service.email.EmailServiceFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Dispatcher;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Log4j2
@Service
public class AuthEventConsumer {

    private final Semaphore limit = new Semaphore(100);
    private final ExecutorService emailExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final ObjectMapper objectMapper;
    private final EmailServiceFactory emailServiceFactory;

    @Autowired
    public AuthEventConsumer(ObjectMapper objectMapper, EmailServiceFactory emailServiceFactory) {
        this.objectMapper = objectMapper;
        this.emailServiceFactory = emailServiceFactory;
    }

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected/reconnected, (re)subscribing to auth events…");
            setupAuthServiceEventSubscriptions(event.connection());
        }
    }

    private void setupAuthServiceEventSubscriptions(Connection nats) {
        Dispatcher dispatcher = nats.createDispatcher(msg -> {
            try {
                String subject = msg.getSubject();
                @SuppressWarnings("unchecked")
                Map<String, String> event = objectMapper.readValue(msg.getData(), Map.class);

                // ack immediately after taking data to avoid redelivery
                msg.ack();

                emailExecutor.submit(() -> {
                    try {
                        limit.acquire();
                        handle(subject, event, emailServiceFactory);
                    } catch (Exception e) {
                        log.error("Error while processing email for subject {}", subject, e);
                        Thread.currentThread().interrupt();
                    } finally {
                        limit.release();
                    }
                });

            } catch (CustomEmailException e) {
                log.error("Business failure sending email, acking anyway", e);
                msg.ack();
            } catch (Exception e) {
                log.error("Critical failure, requesting redelivery", e);
                msg.nak();
            }
        });

        dispatcher.subscribe("auth.>");
        log.info("Subscribed to auth.* events");
    }

    private void handle(String subject, Map<String, String> event, EmailServiceFactory factory) {
        EmailService email = factory.getRotationStrategies().getFirst();

        String toUserEmail = event.get("email");
        String verificationToken = event.get("token");

        switch (subject) {
            case "auth.registration" ->
                    email.sendVerificationEmail(toUserEmail, verificationToken);
            case "auth.forgot_password" ->
                    email.sendForgotPasswordEmail(event.get(toUserEmail), verificationToken);
            case "auth.changed_password" ->
                    email.notifyChangedPassword(
                            event.get(toUserEmail),
                            ZonedDateTime.parse(event.get("timeChangedPassword"))
                    );
            case "auth.otp" -> log.info("OTP event for {} received", event.get("phone"));
            default -> log.warn("Unhandled auth subject: {}", subject);
        }
    }
}