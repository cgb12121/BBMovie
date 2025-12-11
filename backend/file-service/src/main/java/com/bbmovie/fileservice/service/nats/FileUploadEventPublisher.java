package com.bbmovie.fileservice.service.nats;

import com.bbmovie.common.dtos.nats.FileUploadEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
@Service
public class FileUploadEventPublisher extends AbstractNatsJetStreamService implements ReactiveFileUploadEventPublisher {

    private final ObjectMapper objectMapper;

    @Autowired
    public FileUploadEventPublisher(ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> publish(FileUploadEvent event) {
        return Mono.fromCallable(() -> {
                    byte[] data = objectMapper.writeValueAsBytes(event);
                    getJetStream().publish("upload-events", data);
                    return null;
                })
                .onErrorResume(IOException.class, ex -> {
                    // Log the error but don't fail the entire upload process if there are no responders
                    if (ex.getMessage().contains("503 No Responders")) {
                        log.warn("No responders available for upload-events. Event will be queued in outbox for later processing: {}", event.getTitle(), ex);
                        return Mono.empty(); // Return empty Mono to continue the flow without error
                    }
                    log.error("Failed to publish upload event: ", ex);
                    return Mono.error(ex);
                })
                .then();
    }
}