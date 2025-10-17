package com.bbmovie.payment.service.nats;

import com.bbmovie.payment.dto.event.NatsConnectionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.ConnectionListener;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Service
public class PaymentEventProducerImpl implements PaymentEventProducer {

    private final AtomicReference<JetStream> jetStreamRef = new AtomicReference<>();
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentEventProducerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void onNatsConnection(NatsConnectionEvent event) {
        if (event.type() == ConnectionListener.Events.CONNECTED || event.type() == ConnectionListener.Events.RECONNECTED) {
            log.info("NATS connected, initializing JetStream context for PaymentEventProducer.");
            try {
                jetStreamRef.set(event.connection().jetStream());
            } catch (IOException e) {
                log.error("Failed to create JetStream context after NATS connection was established.", e);
            }
        }
    }

    @Override
    public <T> void publish(String subject, T event) {
        JetStream jetStream = jetStreamRef.get();
        if (jetStream == null) {
            log.warn("NATS JetStream is not available. Skipping event publication to subject: {}", subject);
            return;
        }

        try {
            byte[] data = objectMapper.writeValueAsBytes(event);
            PublishAck ack = jetStream.publish(subject, data);
            log.info("Published to subject [{}] with seq: {}", subject, ack.getSeqno());
        } catch (IOException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
        } catch (JetStreamApiException e) {
            log.error("Failed to publish event: {}", e.getMessage());
        }
    }

    @Override
    public <T> void publishSubscriptionSuccessEvent(T data) {
        publish("payments.subscription.success", data);
    }

    @Override
    public <T> void publishSubscriptionCancelEvent(T data) {
        publish("payments.subscription.cancel", data);
    }

    @Override
    public <T> void publishSubscriptionRenewEvent(T data) {
        publish("payments.subscription.renew", data);
    }
}