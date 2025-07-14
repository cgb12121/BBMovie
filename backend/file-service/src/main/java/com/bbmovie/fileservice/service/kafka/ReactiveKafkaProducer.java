package com.bbmovie.fileservice.service.kafka;

import com.example.common.dtos.kafka.FileUploadEvent;
import reactor.core.publisher.Mono;

public interface ReactiveKafkaProducer {
    Mono<Void> send(String topic, String aggregateId, FileUploadEvent event);
}