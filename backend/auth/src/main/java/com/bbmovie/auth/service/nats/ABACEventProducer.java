package com.bbmovie.auth.service.nats;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.nats.client.Connection;
import io.nats.client.JetStream;

@Log4j2
@Service
public class ABACEventProducer {

    private final JetStream jetStream;

    @Autowired
    public ABACEventProducer(Connection nats) throws java.io.IOException {
        this.jetStream = nats.jetStream();
    }

    public void send(String key) {
        try {
            jetStream.publish("auth.abac", key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("send ABAC event error", e);
        }
    }
}
