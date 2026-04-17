package com.bbmovie.email.service.nats;

import com.bbmovie.email.dto.event.NatsConnectionEvent;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@Service
public class NotificationEventConsumer {

    private final ExecutorService emailExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper objectMapper;
    private final EmailServiceFactory emailServiceFactory;

    @Autowired
    public NotificationEventConsumer(ObjectMapper objectMapper, EmailServiceFactory emailServiceFactory) {
        this.objectMapper = objectMapper;
        this.emailServiceFactory = emailServiceFactory;
    }

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected, subscribing to notification events...");
            setupSubscriptions(event.connection());
        }
    }

    private void setupSubscriptions(Connection nats) {
        Dispatcher dispatcher = nats.createDispatcher(msg -> {
            try {
                String subject = msg.getSubject();

                @SuppressWarnings("unchecked")
                Map<String, String> event = objectMapper.readValue(msg.getData(), Map.class);
                
                log.info("Received notification event on subject: {}", subject);

                if ("notifications.email.news".equals(subject)) {
                    emailExecutor.submit(() -> {
                        try {
                            handleNewsEmail(event);
                        } catch (Exception e) {
                            log.error("Error sending news email", e);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Error processing notification event", e);
            }
        });

        dispatcher.subscribe("notifications.email.news");
        log.info("Subscribed to notifications.email.news");
    }

    private void handleNewsEmail(Map<String, String> event) {
        String emailTo = event.get("email");
        String title = event.get("title");
        String content = event.get("content");

        EmailService emailService = emailServiceFactory.getRotationStrategies().getFirst();
        emailService.sendNewsEmail(emailTo, title, content);
        log.info("News email sent to {}", emailTo);
    }
}
