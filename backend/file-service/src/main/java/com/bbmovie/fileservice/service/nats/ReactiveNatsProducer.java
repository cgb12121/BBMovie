package com.bbmovie.fileservice.service.nats;

import com.example.common.dtos.nats.FileUploadEvent;
import reactor.core.publisher.Mono;

public interface ReactiveNatsProducer {
    Mono<Void> send(String topic, String aggregateId, FileUploadEvent event);
}