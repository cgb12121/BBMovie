package com.bbmovie.transcodeworker.service.pipeline.dto;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import io.nats.client.Message;

import java.time.Instant;

/**
 * Data Transfer Object representing a task to be probed.
 * Flows from FetcherStage to ProberStage.
 * <p>
 * Contains all necessary information extracted from NATS message
 * for the prober to determine video metadata and resource cost.
 *
 * @param natsMessage NATS message for ACK/NAK operations
 * @param bucket      MinIO bucket containing the file
 * @param key         MinIO object key (file path)
 * @param purpose     Upload purpose (MOVIE_SOURCE, MOVIE_TRAILER, etc.)
 * @param uploadId    Unique upload identifier from metadata
 * @param contentType Content type of the file (video/mp4, image/jpeg, etc.)
 * @param fileSize    File size in bytes (from MinIO stat)
 * @param fetchedAt   Timestamp when message was fetched (for latency tracking)
 */
public record ProbeTask(
        Message natsMessage,
        String bucket,
        String key,
        UploadPurpose purpose,
        String uploadId,
        String contentType,
        long fileSize,
        Instant fetchedAt
) {
    /**
     * Creates a ProbeTask with current timestamp.
     */
    public static ProbeTask create(
            Message natsMessage,
            String bucket,
            String key,
            UploadPurpose purpose,
            String uploadId,
            String contentType,
            long fileSize) {
        return new ProbeTask(
                natsMessage,
                bucket,
                key,
                purpose,
                uploadId,
                contentType,
                fileSize,
                Instant.now()
        );
    }

    /**
     * Checks if this task is for video processing.
     */
    public boolean isVideo() {
        return purpose == UploadPurpose.MOVIE_SOURCE || purpose == UploadPurpose.MOVIE_TRAILER;
    }

    /**
     * Checks if this task is for image processing.
     */
    public boolean isImage() {
        return purpose == UploadPurpose.USER_AVATAR || purpose == UploadPurpose.MOVIE_POSTER;
    }

    /**
     * Returns time elapsed since fetch in milliseconds.
     */
    public long getQueueTimeMs() {
        return Instant.now().toEpochMilli() - fetchedAt.toEpochMilli();
    }
}

