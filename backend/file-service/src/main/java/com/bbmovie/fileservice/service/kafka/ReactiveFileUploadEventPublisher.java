package com.bbmovie.fileservice.service.kafka;

import com.example.common.dtos.kafka.FileUploadEvent;
import reactor.core.publisher.Mono;

public interface ReactiveFileUploadEventPublisher {
    Mono<Void> publish(FileUploadEvent event);
}