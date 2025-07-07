package com.example.bbmovieuploadfile.service.kafka;

import com.example.common.dtos.kafka.FileUploadEvent;
import reactor.core.publisher.Mono;

public interface ReactiveFileUploadEventPublisher {
    Mono<Void> publish(FileUploadEvent event);
}