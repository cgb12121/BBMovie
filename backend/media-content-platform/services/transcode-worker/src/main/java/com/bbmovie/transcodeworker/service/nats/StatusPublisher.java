package com.bbmovie.transcodeworker.service.nats;

import com.bbmovie.transcodeworker.dto.MediaStatusUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Value("${nats.probe.subject:media.transcode.probe}")
    private String probeSubject;

    // Status constants
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_PROBED = "PROBED";
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
    public void publishStatus(
            String uploadId,
            String status,
            String reason,
            Double duration,
            String filePath,
            List<String> availableResolutions,
            Integer sourceWidth,
            Integer sourceHeight) {
        try {
            List<String> sortedResolutions = sortResolutions(availableResolutions);
            MediaStatusUpdateEvent event = MediaStatusUpdateEvent.builder()
                    .uploadId(uploadId)
                    .status(status)
                    .reason(reason)
                    .duration(duration)
                    .filePath(filePath)
                    .availableResolutions(sortedResolutions)
                    .maxAvailableResolution(maxResolution(sortedResolutions))
                    .sourceWidth(sourceWidth)
                    .sourceHeight(sourceHeight)
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
        publishStatus(uploadId, STATUS_PROCESSING, null, null, null, null, null, null);
    }

    /**
     * Publishes a probe event with source dimensions and planned target resolutions.
     */
    public void publishProbe(String uploadId, Double duration, Integer sourceWidth, Integer sourceHeight, List<String> targetResolutions) {
        try {
            List<String> sortedResolutions = sortResolutions(targetResolutions);
            MediaStatusUpdateEvent event = MediaStatusUpdateEvent.builder()
                    .uploadId(uploadId)
                    .status(STATUS_PROBED)
                    .duration(duration)
                    .availableResolutions(sortedResolutions)
                    .maxAvailableResolution(maxResolution(sortedResolutions))
                    .sourceWidth(sourceWidth)
                    .sourceHeight(sourceHeight)
                    .build();
            String json = objectMapper.writeValueAsString(event);
            natsConnection.publish(probeSubject, json.getBytes(StandardCharsets.UTF_8));
            log.info("Published probe event for {} with resolutions={}", uploadId, sortedResolutions);
        } catch (Exception e) {
            log.error("Failed to publish probe event for uploadId: {}", uploadId, e);
        }
    }

    /**
     * Publishes a completed status with duration.
     */
    public void publishCompleted(String uploadId, Double duration) {
        publishStatus(uploadId, STATUS_COMPLETED, null, duration, null, null, null, null);
    }

    /**
     * Publishes completed status with source metadata and available resolutions.
     */
    public void publishCompleted(
            String uploadId,
            Double duration,
            String filePath,
            List<String> availableResolutions,
            Integer sourceWidth,
            Integer sourceHeight) {
        publishStatus(uploadId, STATUS_COMPLETED, null, duration, filePath, availableResolutions, sourceWidth, sourceHeight);
    }

    /**
     * Publishes a completed status without duration (for images).
     */
    public void publishCompleted(String uploadId) {
        publishStatus(uploadId, STATUS_COMPLETED, null, null, null, null, null, null);
    }

    /**
     * Publishes a failed status with reason.
     */
    public void publishFailed(String uploadId, String reason) {
        publishStatus(uploadId, STATUS_FAILED, reason, null, null, null, null, null);
    }

    /**
     * Publishes a malware detected status.
     */
    public void publishMalwareDetected(String uploadId) {
        publishStatus(uploadId, STATUS_MALWARE_DETECTED, "Malware detected in file", null, null, null, null, null);
    }

    /**
     * Publishes an invalid file status.
     */
    public void publishInvalidFile(String uploadId, String reason) {
        publishStatus(uploadId, STATUS_INVALID_FILE, reason, null, null, null, null, null);
    }

    private static final Pattern RESOLUTION_PATTERN = Pattern.compile("^(\\d+)p$");

    private List<String> sortResolutions(List<String> resolutions) {
        if (resolutions == null || resolutions.isEmpty()) {
            return resolutions;
        }
        return resolutions.stream()
                .filter(r -> r != null && !r.isBlank())
                .distinct()
                .sorted(Comparator.comparingInt(this::resolutionHeight))
                .toList();
    }

    private String maxResolution(List<String> resolutions) {
        if (resolutions == null || resolutions.isEmpty()) {
            return null;
        }
        return resolutions.get(resolutions.size() - 1);
    }

    private int resolutionHeight(String suffix) {
        Matcher matcher = RESOLUTION_PATTERN.matcher(suffix.trim().toLowerCase());
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }
}

