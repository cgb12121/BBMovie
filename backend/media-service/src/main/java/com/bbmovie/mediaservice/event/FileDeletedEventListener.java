package com.bbmovie.mediaservice.event;

import com.bbmovie.mediaservice.entity.MovieStatus;
import com.bbmovie.mediaservice.service.MovieService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileDeletedEventListener {

    private final Connection natsConnection;
    private final MovieService movieService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nats.file.deleted.subject:file.physical.deleted}")
    private String fileDeletedSubject;

    @PostConstruct
    public void init() {
        try {
            Dispatcher dispatcher = natsConnection.createDispatcher();
            dispatcher.subscribe(fileDeletedSubject, this::handleFileDeletedEvent);
            log.info("Subscribed to NATS subject: {}", fileDeletedSubject);
        } catch (Exception e) {
            log.error("Failed to initialize NATS file deleted listener", e);
            throw new RuntimeException("Failed to initialize NATS file deleted listener", e);
        }
    }

    private void handleFileDeletedEvent(Message msg) {
        try {
            String messageData = new String(msg.getData());
            log.info("Received file deleted event: {}", messageData);

            // Parse JSON
            JsonNode eventNode = objectMapper.readTree(messageData);

            String fileId = eventNode.path("fileId").asText();
            if (fileId == null || fileId.isEmpty()) {
                // Try alternative field names
                fileId = eventNode.path("file_id").asText();
                if (fileId == null || fileId.isEmpty()) {
                    log.warn("No fileId found in file deleted event: {}", messageData);
                    return;
                }
            }

            // Find movie associated with this file and mark it as ERROR/MISSING_FILE
            String finalFileId = fileId;
            movieService.updateMovieStatusByFileId(fileId, MovieStatus.ERROR)
                .ifPresent(movie -> {
                    log.warn("File {} was deleted, marked associated movie {} as ERROR", finalFileId, movie.getMovieId());
                });

        } catch (Exception e) {
            log.error("Error processing file deleted event", e);
        }
    }
}
