package com.bbmovie.fileservice.service.nats;

import com.example.common.dtos.nats.FileUploadEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Service
public class ReactiveNatsProducerImpl implements ReactiveNatsProducer {

    private final JetStream jetStream;
    private final ObjectMapper objectMapper;

    @Autowired
    public ReactiveNatsProducerImpl(Connection nats, ObjectMapper objectMapper) throws IOException {
        this.jetStream = nats.jetStream();
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> send(String subject, String aggregateId, FileUploadEvent event) {
        return Mono.fromCallable(() -> {
                    byte[] data = objectMapper.writeValueAsBytes(event);
                    jetStream.publish(subject, data);
                    return null;
                })
                .then();
    }
}