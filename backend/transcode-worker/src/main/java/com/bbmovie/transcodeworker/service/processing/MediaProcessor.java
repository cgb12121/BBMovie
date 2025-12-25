package com.bbmovie.transcodeworker.service.processing;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.pipeline.dto.ExecuteTask;

import java.nio.file.Path;

/**
 * Interface for media processing strategies.
 * Implementations handle specific media types (video, image).
 */
public interface MediaProcessor {

    /**
     * Checks if this processor can handle the given purpose.
     */
    boolean supports(UploadPurpose purpose);

    /**
     * Processes the media file.
     *
     * @param task      The execute task with metadata
     * @param inputFile Downloaded input file
     * @param outputDir Output directory for results
     * @return Processing result with output info
     */
    ProcessingResult process(ExecuteTask task, Path inputFile, Path outputDir);

    /**
     * Result of media processing.
     */
    record ProcessingResult(
            Double duration,        // Video duration (null for images)
            String outputPath,      // Path to output directory
            boolean success,
            String errorMessage
    ) {
        public static ProcessingResult success(Double duration, String outputPath) {
            return new ProcessingResult(duration, outputPath, true, null);
        }

        public static ProcessingResult success(String outputPath) {
            return new ProcessingResult(null, outputPath, true, null);
        }

        public static ProcessingResult failure(String errorMessage) {
            return new ProcessingResult(null, null, false, errorMessage);
        }
    }
}

