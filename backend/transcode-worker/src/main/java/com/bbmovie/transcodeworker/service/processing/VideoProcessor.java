package com.bbmovie.transcodeworker.service.processing;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;
import com.bbmovie.transcodeworker.service.ffmpeg.VideoTranscoderService;
import com.bbmovie.transcodeworker.service.pipeline.dto.ExecuteTask;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeResult;
import com.bbmovie.transcodeworker.service.storage.MinioUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Processor for video files.
 * Handles transcoding to HLS format with multiple resolutions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessor implements MediaProcessor {

    @Value("${app.minio.hls-bucket}")
    private String hlsBucket;

    @Value("${app.minio.secure-bucket}")
    private String secureBucket;

    private final ValidationService validationService;
    private final VideoTranscoderService videoTranscoderService;
    private final MinioUploadService uploadService;

    /** Purposes that this processor handles */
    private static final Set<UploadPurpose> SUPPORTED_PURPOSES = Set.of(
            UploadPurpose.MOVIE_SOURCE,
            UploadPurpose.MOVIE_TRAILER
    );

    @Override
    public boolean supports(UploadPurpose purpose) {
        return SUPPORTED_PURPOSES.contains(purpose);
    }

    @Override
    public ProcessingResult process(ExecuteTask task, Path inputFile, Path outputDir) {
        String taskId = task.uploadId();
        log.info("Processing video: {} ({}x{})", taskId, task.probeResult().width(), task.probeResult().height());

        try {
            // 1. Validate file
            validationService.validate(inputFile, task.purpose());

            // 2. Convert ProbeResult to FFmpegVideoMetadata
            ProbeResult probe = task.probeResult();
            FFmpegVideoMetadata metadata = new FFmpegVideoMetadata(
                    probe.width(),
                    probe.height(),
                    probe.duration(),
                    probe.codec()
            );

            // 3. Determine resolutions
            List<VideoTranscoderService.VideoResolution> resolutions =
                    videoTranscoderService.determineTargetResolutions(metadata);

            // 4. Transcode to HLS
            Path hlsOutputDir = outputDir.resolve("hls");
            videoTranscoderService.transcode(
                    inputFile,
                    resolutions,
                    hlsOutputDir.toString(),
                    taskId,
                    metadata
            );

            // 5. Upload results to MinIO
            uploadHlsOutput(task, hlsOutputDir);

            log.info("Video processing completed: {} (video duration: {}s)", taskId, probe.duration());
            return ProcessingResult.success(probe.duration(), hlsOutputDir.toString());

        } catch (Exception e) {
            log.error("Video processing failed: {}", taskId, e);
            return ProcessingResult.failure(e.getMessage());
        }
    }

    /**
     * Uploads HLS output to MinIO.
     * - Segments (.ts, .m3u8) go to HLS bucket (public)
     * - Keys (.key) go to Secure bucket (protected)
     */
    private void uploadHlsOutput(ExecuteTask task, Path hlsOutputDir) {
        String destination = getDestinationPath(task);

        // Upload segments and playlists to HLS bucket (exclude .key files)
        uploadService.uploadDirectoryFiltered(
                hlsOutputDir,
                hlsBucket,
                destination,
                path -> !path.toString().endsWith(".key")
        );
        log.debug("Uploaded HLS segments to: {}/{}", hlsBucket, destination);

        // Upload encryption keys to Secure bucket
        uploadService.uploadDirectoryFiltered(
                hlsOutputDir,
                secureBucket,
                destination,
                path -> path.toString().endsWith(".key")
        );
        log.debug("Uploaded encryption keys to: {}/{}", secureBucket, destination);
    }

    /**
     * Determines the destination path based on purpose.
     */
    private String getDestinationPath(ExecuteTask task) {
        return switch (task.purpose()) {
            case MOVIE_SOURCE -> "movies/" + task.uploadId();
            case MOVIE_TRAILER -> "trailers/" + task.uploadId();
            default -> throw new IllegalArgumentException("Unsupported purpose: " + task.purpose());
        };
    }
}

