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
public class TranscodeEventListener {

    private final Connection natsConnection;
    private final MovieService movieService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nats.transcode.subject:media.status.update}")
    private String transcodeSubject;

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

            // Parse JSON properly
            JsonNode eventNode = objectMapper.readTree(messageData);

            String status = eventNode.path("status").asText();
            
            // Transcode worker sends uploadId, which is the same as fileId in upload-service
            // Try fileId first, then uploadId as fallback
            String fileId = eventNode.path("fileId").asText();
            if (fileId == null || fileId.isEmpty()) {
                fileId = eventNode.path("file_id").asText();
            }
            if (fileId == null || fileId.isEmpty()) {
                // Fallback to uploadId (they are the same UUID in upload-service)
                fileId = eventNode.path("uploadId").asText();
            }
            
            if (fileId == null || fileId.isEmpty()) {
                log.warn("No fileId/uploadId found in transcode event: {}", messageData);
                return;
            }

            // Extract filePath if available (for HLS, it's the path to master.m3u8)
            String filePath = eventNode.path("filePath").asText();
            if (filePath == null || filePath.isEmpty()) {
                // Construct default HLS path based on uploadId
                filePath = "movies/" + fileId + "/master.m3u8";
            }

            // Extract duration (in seconds) and convert to minutes
            Double durationSeconds = null;
            if (eventNode.has("duration") && !eventNode.path("duration").isNull()) {
                durationSeconds = eventNode.path("duration").asDouble();
            }
            Integer durationMinutes = null;
            if (durationSeconds != null && durationSeconds > 0) {
                // Convert seconds to minutes (round up)
                durationMinutes = (int) Math.ceil(durationSeconds / 60.0);
            }

            log.info("Processing transcode event: fileId={}, status={}, filePath={}, duration={}s ({}min)", 
                    fileId, status, filePath, durationSeconds, durationMinutes);

            // StatusPublisher sends: COMPLETED, FAILED, MALWARE_DETECTED, INVALID_FILE
            if ("COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status) || "READY".equalsIgnoreCase(status)) {
                // Find movie by fileId and update status to PUBLISHED
                String finalFileId = fileId;
                String finalFilePath = filePath;
                Integer finalDuration = durationMinutes;
                
                movieService.updateMovieStatusByFileId(fileId, MovieStatus.PUBLISHED)
                    .ifPresent(movie -> {
                        // Update file path if not already set
                        if (movie.getFilePath() == null || movie.getFilePath().isEmpty()) {
                            movieService.updateMovieFilePath(movie.getMovieId(), finalFilePath);
                        }
                        // Update duration if available and not already set
                        if (finalDuration != null && (movie.getDuration() == null || movie.getDuration() == 0)) {
                            movieService.updateMovieDuration(movie.getMovieId(), finalDuration);
                        }
                        log.info("Updated movie {} status to PUBLISHED for file ID: {} (duration: {}min)", 
                                movie.getMovieId(), finalFileId, finalDuration);
                        // MovieService will handle publishing MoviePublishedEvent if status changes to PUBLISHED
                    });
            } else if ("FAILED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) 
                    || "MALWARE_DETECTED".equalsIgnoreCase(status) || "INVALID_FILE".equalsIgnoreCase(status)) {
                String finalFileId = fileId;
                movieService.updateMovieStatusByFileId(fileId, MovieStatus.ERROR)
                    .ifPresent(movie -> log.info("Updated movie {} status to ERROR for file ID: {}", movie.getMovieId(), finalFileId));
            } else if ("PROCESSING".equalsIgnoreCase(status) || "TRANSCODING".equalsIgnoreCase(status)) {
                // Intermediate status - just log, no action needed
                log.debug("Transcode in progress for file ID: {}", fileId);
            } else {
                log.warn("Unknown transcode status: {} for file ID: {}", status, fileId);
            }
        } catch (Exception e) {
            log.error("Error processing transcode event", e);
        }
    }

}