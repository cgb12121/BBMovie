package com.bbmovie.payment.service.messaging;

import com.example.common.dtos.nats.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//TODO: move to email service
@Log4j2
@Service
public class EmailConsumer {

    public EmailConsumer(Connection nats, ObjectMapper objectMapper) throws IOException, JetStreamApiException {
        JetStream js = nats.jetStream();

        JetStreamSubscription sub = js.subscribe(
            "payment.success",
            PullSubscribeOptions.builder()
                    .durable("email-service")
                    .build()
        );

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try {
                    while (true) {
                        sub.fetch(100, Duration.ofSeconds(10)).forEach(msg -> {
                            try {
                                PaymentEvent event = objectMapper.readValue(msg.getData(), PaymentEvent.class);
                                log.info("[Virtual Thread] Email sent to {}", event.userEmail());
                                msg.ack();
                            } catch (Exception e) {
                                msg.nak();
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("Error processing messages", e);
                }
            });
        }
    }
}
