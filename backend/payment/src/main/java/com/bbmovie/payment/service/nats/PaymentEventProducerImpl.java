package com.bbmovie.payment.service.nats;

import com.bbmovie.payment.config.NatsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Log4j2
@Service
public class PaymentEventProducerImpl implements PaymentEventProducer {

    private final JetStream jetStream;
    private final ObjectMapper objectMapper;

    @Autowired
    public PaymentEventProducerImpl(NatsConfig.NatsConnectionFactory natsConnectionFactory, ObjectMapper objectMapper) throws IOException {
        this.jetStream = natsConnectionFactory.getConnection().jetStream();
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> void publish(String subject, T event) {
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
        publish("payment.subscription.success", data);
    }

    @Override
    public <T> void publishSubscriptionCancelEvent(T data) {
        publish("payment.subscription.cancel", data);
    }

    @Override
    public <T> void publishSubscriptionRenewEvent(T data) {
        publish("payment.subscription.renew", data);
    }
}