package com.bbmovie.transcodeworker.service.nats;

import com.bbmovie.transcodeworker.dto.MediaStatusUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Publishes status updates to NATS messaging system.
 * <p>
 * Extracted from MediaEventConsumer to follow Single Responsibility Principle.
 * Handles all status publishing concerns for media processing events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusPublisher {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    @Value("${nats.status.subject:media.status.update}")
    private String statusSubject;

    // Status constants
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_MALWARE_DETECTED = "MALWARE_DETECTED";
    public static final String STATUS_INVALID_FILE = "INVALID_FILE";

    /**
     * Publishes a status update event.
     *
     * @param uploadId Unique identifier for the upload
     * @param status   Current status (PROCESSING, COMPLETED, FAILED, etc.)
     * @param reason   Reason for the status (particularly for errors)
     * @param duration Video duration in seconds (null for non-video or errors)
     */
    public void publishStatus(String uploadId, String status, String reason, Double duration) {
        try {
            MediaStatusUpdateEvent event = MediaStatusUpdateEvent.builder()
                    .uploadId(uploadId)
                    .status(status)
                    .reason(reason)
                    .duration(duration)
                    .build();

            String json = objectMapper.writeValueAsString(event);
            natsConnection.publish(statusSubject, json.getBytes(StandardCharsets.UTF_8));
            log.info("Published status update: {} for {} (duration: {}s)", status, uploadId, duration);
        } catch (Exception e) {
            log.error("Failed to publish status update for uploadId: {}", uploadId, e);
        }
    }

    /**
     * Publishes a processing started status.
     */
    public void publishProcessing(String uploadId) {
        publishStatus(uploadId, STATUS_PROCESSING, null, null);
    }

    /**
     * Publishes a completed status with duration.
     */
    public void publishCompleted(String uploadId, Double duration) {
        publishStatus(uploadId, STATUS_COMPLETED, null, duration);
    }

    /**
     * Publishes a completed status without duration (for images).
     */
    public void publishCompleted(String uploadId) {
        publishStatus(uploadId, STATUS_COMPLETED, null, null);
    }

    /**
     * Publishes a failed status with reason.
     */
    public void publishFailed(String uploadId, String reason) {
        publishStatus(uploadId, STATUS_FAILED, reason, null);
    }

    /**
     * Publishes a malware detected status.
     */
    public void publishMalwareDetected(String uploadId) {
        publishStatus(uploadId, STATUS_MALWARE_DETECTED, "Malware detected in file", null);
    }

    /**
     * Publishes an invalid file status.
     */
    public void publishInvalidFile(String uploadId, String reason) {
        publishStatus(uploadId, STATUS_INVALID_FILE, reason, null);
    }
}

