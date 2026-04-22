package com.bbmovie.transcodeworker.service.pipeline.dto;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.scheduler.TranscodeScheduler.ResourceHandle;
import io.nats.client.Message;

import java.time.Instant;

/**
 * Data Transfer Object representing a task ready for execution.
 * Flows from ProberStage to ExecutorStage.
 * <p>
 * Contains all information needed to execute the transcoding/processing job,
 * including pre-acquired scheduler resources.
 *
 * @param natsMessage   NATS message for ACK operations after completion
 * @param bucket        MinIO bucket containing the file
 * @param key           MinIO object key (file path)
 * @param purpose       Upload purpose (MOVIE_SOURCE, MOVIE_TRAILER, etc.)
 * @param uploadId      Unique upload identifier
 * @param probeResult   Probing results (resolution, codec, target resolutions, costs)
 * @param resourceHandle Pre-acquired scheduler resources (MUST be released after execution)
 * @param probedAt      Timestamp when probing completed
 */
public record ExecuteTask(
        Message natsMessage,
        String bucket,
        String key,
        UploadPurpose purpose,
        String uploadId,
        ProbeResult probeResult,
        ResourceHandle resourceHandle,
        Instant probedAt
) {
    /**
     * Creates an ExecuteTask from a ProbeTask and its results.
     */
    public static ExecuteTask from(
            ProbeTask probeTask,
            ProbeResult probeResult,
            ResourceHandle resourceHandle) {
        return new ExecuteTask(
                probeTask.natsMessage(),
                probeTask.bucket(),
                probeTask.key(),
                probeTask.purpose(),
                probeTask.uploadId(),
                probeResult,
                resourceHandle,
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
     * Returns total time spent in pipeline (fetch + probe + queue time) in milliseconds.
     */
    public long getPipelineTimeMs() {
        return Instant.now().toEpochMilli() - probedAt.toEpochMilli();
    }

    /**
     * Returns the number of actual threads to use for FFmpeg.
     * This is clamped based on scheduler capacity.
     */
    public int getActualThreads() {
        return resourceHandle != null ? resourceHandle.getActualThreads() : 1;
    }

    /**
     * Acknowledges the NATS message (call after successful processing).
     */
    public void ack() {
        if (natsMessage != null) {
            natsMessage.ack();
        }
    }

    /**
     * Negative acknowledges the NATS message (call on failure for redelivery).
     */
    public void nak() {
        if (natsMessage != null) {
            natsMessage.nak();
        }
    }
}

