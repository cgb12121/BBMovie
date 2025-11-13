package com.bbmovie.fileservice.service.nats;

import com.bbmovie.common.dtos.nats.FileUploadEvent;
import reactor.core.publisher.Mono;

public interface ReactiveFileUploadEventPublisher {
    Mono<Void> publish(FileUploadEvent event);
}