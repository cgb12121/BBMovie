package com.bbmovie.fileservice.service.nats;

import com.example.common.dtos.nats.FileUploadEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReactiveNatsProducerImpl extends AbstractNatsJetStreamService implements ReactiveNatsProducer {

    private final ObjectMapper objectMapper;

    @Autowired
    public ReactiveNatsProducerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> send(String subject, String aggregateId, FileUploadEvent event) {
        return Mono.fromCallable(() -> {
                    byte[] data = objectMapper.writeValueAsBytes(event);
                    getJetStream().publish(subject, data);
                    return null;
                })
                .then();
    }
}