package com.bbmovie.notificationservice.service.nats;

import com.bbmovie.notificationservice.dto.event.NatsConnectionEvent;
import com.bbmovie.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Dispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class NewsEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected, subscribing to news events...");
            setupSubscriptions(event.connection());
        }
    }

    private void setupSubscriptions(Connection nats) {
        Dispatcher dispatcher = nats.createDispatcher(msg -> {
            try {
                String subject = msg.getSubject();
                log.info("Received event on subject: {}", subject);

                @SuppressWarnings("all")
                Map<String, Object> event = objectMapper.readValue(msg.getData(), Map.class);

                if ("news.important.published".equals(subject)) {
                    notificationService.processNews(event);
                }
            } catch (Exception e) {
                log.error("Error processing news event", e);
            }
        });

        dispatcher.subscribe("news.important.published");
        log.info("Subscribed to news.important.published");
    }
}
