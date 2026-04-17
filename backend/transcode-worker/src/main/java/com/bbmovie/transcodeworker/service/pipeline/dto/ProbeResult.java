package com.bbmovie.transcodeworker.service.pipeline.dto;

import com.bbmovie.transcodeworker.service.complexity.dto.RecipeHints;

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
 * @param recipeHints       CAS hints for LGS (nullable when CAS not applied)
 */
public record ProbeResult(
        int width,
        int height,
        double duration,
        String codec,
        List<String> targetResolutions,
        int peakCost,
        int totalCost,
        RecipeHints recipeHints
) {
    public static ProbeResult forImage(int width, int height) {
        return new ProbeResult(
                width,
                height,
                0.0,
                "image",
                List.of(),
                1,
                1,
                null
        );
    }

    public static ProbeResult forVideo(
            int width,
            int height,
            double duration,
            String codec,
            List<String> targetResolutions,
            int peakCost,
            int totalCost) {
        return forVideo(width, height, duration, codec, targetResolutions, peakCost, totalCost, null);
    }

    public static ProbeResult forVideo(
            int width,
            int height,
            double duration,
            String codec,
            List<String> targetResolutions,
            int peakCost,
            int totalCost,
            RecipeHints recipeHints) {
        return new ProbeResult(
                width,
                height,
                duration,
                codec,
                targetResolutions,
                peakCost,
                totalCost,
                recipeHints
        );
    }

    public String getOriginalResolution() {
        return width + "x" + height;
    }

    public boolean isHighResolution() {
        return height >= 720;
    }

    public boolean is4K() {
        return height >= 2160;
    }

    public long estimatedProcessingTimeSeconds() {
        double multiplier = height >= 1080 ? 4.0 : (height >= 720 ? 2.0 : 1.0);
        return (long) (duration * multiplier);
    }
}
