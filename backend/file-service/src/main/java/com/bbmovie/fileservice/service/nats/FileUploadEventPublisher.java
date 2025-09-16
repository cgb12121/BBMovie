package com.bbmovie.fileservice.service.nats;

import com.example.common.dtos.nats.FileUploadEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
@Service
public class FileUploadEventPublisher implements ReactiveFileUploadEventPublisher {

    private final JetStream jetStream;
    private final ObjectMapper objectMapper;

    @Autowired
    public FileUploadEventPublisher(Connection nats, ObjectMapper objectMapper) throws IOException {
        this.jetStream = nats.jetStream();
        this.objectMapper = objectMapper;
    }

    public Mono<Void> publish(FileUploadEvent event) {
        return Mono.fromCallable(() -> {
                    byte[] data = objectMapper.writeValueAsBytes(event);
                    jetStream.publish("upload-events", data);
                    return null;
                })
                .then();
    }
}