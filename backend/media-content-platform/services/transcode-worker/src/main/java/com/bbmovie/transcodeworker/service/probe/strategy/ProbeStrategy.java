package com.bbmovie.transcodeworker.service.probe.strategy;

import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeResult;

/**
 * Strategy interface for probing video metadata.
 * <p>
 * Different strategies can be used based on network conditions,
 * file size, and MinIO configuration.
 */
public interface ProbeStrategy {

    /**
     * Returns the name of this strategy for logging.
     *
     * @return Strategy name
     */
    String getName();

    /**
     * Checks if this strategy can be used for the given object.
     *
     * @param bucket MinIO bucket
     * @param key    Object key
     * @return true if this strategy supports the given object
     */
    boolean supports(String bucket, String key);

    /**
     * Probes the video file and returns metadata.
     *
     * @param bucket MinIO bucket
     * @param key    Object key
     * @return ProbeResult with video metadata and cost calculation
     * @throws ProbeException if probing fails
     */
    ProbeResult probe(String bucket, String key) throws ProbeException;

    /**
     * Returns the priority of this strategy (higher = preferred).
     * The default implementation returns 0.
     *
     * @return Priority value
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Exception thrown when probing fails.
     */
    class ProbeException extends RuntimeException {
        public ProbeException(String message) {
            super(message);
        }

        public ProbeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

