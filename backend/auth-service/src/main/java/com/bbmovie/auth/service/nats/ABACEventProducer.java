package com.bbmovie.auth.service.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStreamApiException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.nats.client.JetStream;

import java.io.IOException;

@Log4j2
@Service
public class ABACEventProducer extends AbstractNatsJetStreamService {

    private final ObjectMapper objectMapper;

    @Autowired
    public ABACEventProducer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void send(String key) {
        JetStream jetStream = getJetStream();
        try {
            byte[] data = objectMapper.writeValueAsBytes(key);
            jetStream.publish("auth.abac", data);
        } catch (IOException e) {
            log.error("Failed to serialize health event: {}", e.getMessage());
        } catch (JetStreamApiException e) {
            log.error("Failed to publish health event: {}", e.getMessage());
        }
    }
}
