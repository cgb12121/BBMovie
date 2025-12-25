package com.bbmovie.transcodeworker.service.pipeline.dto;

import java.util.List;

/**
 * Result of the probing operation.
 * Contains video metadata and calculated resource costs.
 * <p>
 * This DTO eliminates duplicate MetadataService calls by passing
 * probe results from ProberStage to ExecutorStage.
 *
 * @param width             Video width in pixels
 * @param height            Video height in pixels
 * @param duration          Video duration in seconds
 * @param codec             Video codec (e.g., "h264", "hevc")
 * @param targetResolutions List of resolution suffixes to transcode to (e.g., "1080p", "720p")
 * @param peakCost          Maximum cost of any single resolution (for scheduling)
 * @param totalCost         Sum of all resolution costs
 */
public record ProbeResult(
        int width,
        int height,
        double duration,
        String codec,
        List<String> targetResolutions,
        int peakCost,
        int totalCost
) {
    /**
     * Creates a ProbeResult for image files (no video metadata needed).
     */
    public static ProbeResult forImage(int width, int height) {
        return new ProbeResult(
                width,
                height,
                0.0,
                "image",
                List.of(),
                1, // Images have minimal cost
                1
        );
    }

    /**
     * Creates a ProbeResult for video files with full metadata.
     */
    public static ProbeResult forVideo(
            int width,
            int height,
            double duration,
            String codec,
            List<String> targetResolutions,
            int peakCost,
            int totalCost) {
        return new ProbeResult(
                width,
                height,
                duration,
                codec,
                targetResolutions,
                peakCost,
                totalCost
        );
    }

    /**
     * Returns the original resolution string (e.g., "1920x1080").
     */
    public String getOriginalResolution() {
        return width + "x" + height;
    }

    /**
     * Checks if this is a high-resolution video (720p or above).
     */
    public boolean isHighResolution() {
        return height >= 720;
    }

    /**
     * Checks if this is a 4K video.
     */
    public boolean is4K() {
        return height >= 2160;
    }

    /**
     * Returns estimated processing time based on duration and resolution.
     * This is an estimate for logging/monitoring purposes.
     */
    public long estimatedProcessingTimeSeconds() {
        // Rough estimate: 1x realtime for 480p, 2x for 720p, 4x for 1080p
        double multiplier = height >= 1080 ? 4.0 : (height >= 720 ? 2.0 : 1.0);
        return (long) (duration * multiplier);
    }
}

