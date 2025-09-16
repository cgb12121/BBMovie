package com.bbmovie.email.service.nats;

import com.bbmovie.email.service.email.EmailService;
import com.bbmovie.email.service.email.EmailServiceFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;

@Log4j2
@Service
public class AuthEventConsumer {

    @Autowired
    public AuthEventConsumer(Connection nats, ObjectMapper objectMapper, EmailServiceFactory emailServiceFactory) throws Exception {
        JetStream js = nats.jetStream();

        // Durable consumers are configured in NatsConfig for stream binding
        Dispatcher dispatcher = nats.createDispatcher(msg -> {
            try {
                String subject = msg.getSubject();
                Map event = objectMapper.readValue(msg.getData(), Map.class);
                handle(subject, event, emailServiceFactory);
                msg.ack();
            } catch (Exception e) {
                log.error("Failed to process auth event", e);
                msg.nak();
            }
        });

        dispatcher.subscribe("auth.>");
    }

    private void handle(String subject, Map<String, String> event, EmailServiceFactory factory) {
        EmailService email = factory.getRotationStrategies().getFirst();
        try {
            switch (subject) {
                case "auth.registration" -> email.sendVerificationEmail(event.get("email"), event.get("token"));
                case "auth.forgot_password" -> email.sendForgotPasswordEmail(event.get("email"), event.get("token"));
                case "auth.changed_password" -> email.notifyChangedPassword(event.get("email"), ZonedDateTime.parse(event.get("timeChangedPassword")));
                case "auth.otp" -> log.info("OTP event for {} received", event.get("phone"));
                default -> log.warn("Unhandled auth subject: {}", subject);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


