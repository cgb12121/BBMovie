package com.bbmovie.payment.service.event.messaging;

import com.example.common.dtos.nats.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.api.PublishAck;
import io.nats.client.JetStream;
import io.nats.client.Nats;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Log4j2
@Service
public class PaymentProducer {

    private final JetStream jetStream;
    private final ObjectMapper objectMapper;

    public PaymentProducer(Connection nats, ObjectMapper objectMapper) throws IOException {
        this.jetStream = nats.jetStream();
        this.objectMapper = objectMapper;
    }

    public void publishEvent(PaymentEvent event) throws Exception {
        byte[] data = objectMapper.writeValueAsBytes(event);
        PublishAck ack = jetStream.publish("payment.success", data);
        log.info("Published with seq: {}" , ack.getSeqno());
    }
}
