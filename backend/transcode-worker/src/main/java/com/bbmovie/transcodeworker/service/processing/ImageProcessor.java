package com.bbmovie.transcodeworker.service.processing;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.ffmpeg.ImageProcessingService;
import com.bbmovie.transcodeworker.service.pipeline.dto.ExecuteTask;
import com.bbmovie.transcodeworker.service.storage.MinioUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Processor for image files.
 * Handles resizing and format conversion for avatars, posters, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageProcessor implements MediaProcessor {

    @Value("${app.minio.public-bucket}")
    private String publicBucket;

    private final ValidationService validationService;
    private final ImageProcessingService imageProcessingService;
    private final MinioUploadService uploadService;

    /** Purposes that this processor handles */
    private static final Set<UploadPurpose> SUPPORTED_PURPOSES = Set.of(
            UploadPurpose.USER_AVATAR,
            UploadPurpose.MOVIE_POSTER
    );

    @Override
    public boolean supports(UploadPurpose purpose) {
        return SUPPORTED_PURPOSES.contains(purpose);
    }

    @Override
    public ProcessingResult process(ExecuteTask task, Path inputFile, Path outputDir) {
        String taskId = task.uploadId();
        log.info("Processing image: {} for purpose: {}", taskId, task.purpose());

        try {
            // 1. Validate file
            validationService.validate(inputFile, task.purpose());

            // 2. Determine format from input file
            String format = getFormat(task.key());

            // 3. Process image (resize to multiple sizes)
            Path imageOutputDir = outputDir.resolve("images");
            List<Path> outputs = imageProcessingService.processImageHierarchy(
                    inputFile,
                    imageOutputDir.toString(),
                    format,
                    task.purpose()
            );

            // 4. Upload results to MinIO
            uploadImageOutput(task, imageOutputDir);

            log.info("Image processing completed: {} ({} sizes generated)", taskId, outputs.size());
            return ProcessingResult.success(imageOutputDir.toString());

        } catch (Exception e) {
            log.error("Image processing failed: {}", taskId, e);
            return ProcessingResult.failure(e.getMessage());
        }
    }

    /**
     * Gets the output format from the file key.
     */
    private String getFormat(String key) {
        int lastDot = key.lastIndexOf('.');
        if (lastDot > 0) {
            String ext = key.substring(lastDot + 1).toLowerCase();
            // Normalize extensions
            return switch (ext) {
                case "jpeg" -> "jpg";
                case "webp", "png", "jpg" -> ext;
                default -> throw new IllegalArgumentException("Unknown format: " + ext);
            };
        }
        return "jpg";
    }

    /**
     * Uploads image output to MinIO.
     */
    private void uploadImageOutput(ExecuteTask task, Path imageOutputDir) {
        String destination = getDestinationPath(task);
        uploadService.uploadDirectory(imageOutputDir, publicBucket, destination);
        log.debug("Uploaded images to: {}/{}", publicBucket, destination);
    }
    /**
     * Determines the destination path based on purpose.
     */
    private String getDestinationPath(ExecuteTask task) {
        return switch (task.purpose()) {
            case USER_AVATAR -> "avatars/" + task.uploadId();
            case MOVIE_POSTER -> "movies/" + task.uploadId() + "/poster";
            default -> "other/" + task.uploadId();
        };
    }
}

