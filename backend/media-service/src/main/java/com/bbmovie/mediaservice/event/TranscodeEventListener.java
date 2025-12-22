package com.bbmovie.mediaservice.event;

import com.bbmovie.mediaservice.entity.MovieStatus;
import com.bbmovie.mediaservice.service.MovieService;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscodeEventListener {

    private final Connection natsConnection;
    private final MovieService movieService;

    @Value("${nats.transcode.subject:media.status.update}")
    private String transcodeSubject;

    @Value("${nats.movie.published.subject:movie.published}")
    private String moviePublishedSubject;

    @PostConstruct
    public void init() {
        try {
            Dispatcher dispatcher = natsConnection.createDispatcher();
            dispatcher.subscribe(transcodeSubject, this::handleTranscodeEvent);
            log.info("Subscribed to NATS subject: {}", transcodeSubject);
        } catch (Exception e) {
            log.error("Failed to initialize NATS listener", e);
            throw new RuntimeException("Failed to initialize NATS listener", e);
        }
    }

    private void handleTranscodeEvent(Message msg) {
        try {
            String messageData = new String(msg.getData());
            log.info("Received transcode event: {}", messageData);

            // Parse the event data - in a real implementation, you'd deserialize the JSON
            // For now, let's assume the message contains file_id and status
            if (messageData.contains("\"status\":\"READY\"") || messageData.contains("\"status\":\"SUCCESS\"")) {
                // Extract file_id from the message - this is a simplified approach
                // In a real implementation, you'd properly parse the JSON
                String fileId = extractFileId(messageData);
                if (fileId != null && !fileId.isEmpty()) {
                    // Find the movie associated with this file ID
                    movieService.updateMovieStatusByFileId(fileId, MovieStatus.PUBLISHED)
                        .ifPresent(movie -> {
                            log.info("Updated movie status to PUBLISHED for file ID: {}", fileId);
                            // Publish MoviePublishedEvent to notify other services
                            publishMoviePublishedEvent(movie.getMovieId(), movie.getTitle(), movie.getFilePath());
                        });
                }
            } else if (messageData.contains("\"status\":\"FAILED\"") || messageData.contains("\"status\":\"REJECTED\"")) {
                String fileId = extractFileId(messageData);
                if (fileId != null && !fileId.isEmpty()) {
                    movieService.updateMovieStatusByFileId(fileId, MovieStatus.ERROR)
                        .ifPresent(movie -> {
                            log.info("Updated movie status to ERROR for file ID: {}", fileId);
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error processing transcode event", e);
        }
    }

    private String extractFileId(String messageData) {
        // This is a simplified extraction - in real implementation, properly parse JSON
        // Looking for patterns like "fileId": "some-id" or "file_id": "some-id"
        if (messageData.contains("\"fileId\"")) {
            int start = messageData.indexOf("\"fileId\"") + 9;
            int quoteStart = messageData.indexOf("\"", start);
            if (quoteStart != -1) {
                int quoteEnd = messageData.indexOf("\"", quoteStart + 1);
                if (quoteEnd != -1) {
                    return messageData.substring(quoteStart + 1, quoteEnd);
                }
            }
        } else if (messageData.contains("\"file_id\"")) {
            int start = messageData.indexOf("\"file_id\"") + 10;
            int quoteStart = messageData.indexOf("\"", start);
            if (quoteStart != -1) {
                int quoteEnd = messageData.indexOf("\"", quoteStart + 1);
                if (quoteEnd != -1) {
                    return messageData.substring(quoteStart + 1, quoteEnd);
                }
            }
        }
        return null;
    }

    public void publishMoviePublishedEvent(UUID movieId, String title, String filePath) {
        try {
            String eventPayload = String.format(
                "{\"movieId\":\"%s\",\"title\":\"%s\",\"filePath\":\"%s\",\"timestamp\":\"%s\"}",
                movieId, title, filePath, System.currentTimeMillis()
            );
            natsConnection.publish(moviePublishedSubject, eventPayload.getBytes());
            log.info("Published movie published event for movie: {}", movieId);
        } catch (Exception e) {
            log.error("Failed to publish movie published event", e);
        }
    }
}