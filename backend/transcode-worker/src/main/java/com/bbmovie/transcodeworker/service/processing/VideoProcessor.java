package com.bbmovie.transcodeworker.service.processing;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.port.ComplexityAnalysisPort;
import com.bbmovie.transcodeworker.port.EncodeValidationPort;
import com.bbmovie.transcodeworker.port.VideoQualityPort;
import com.bbmovie.transcodeworker.service.analysis.AnalysisEventPublisher;
import com.bbmovie.transcodeworker.service.analysis.AnalysisPersistenceService;
import com.bbmovie.transcodeworker.service.complexity.dto.ComplexityProfile;
import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;
import com.bbmovie.transcodeworker.service.ffmpeg.VideoTranscoderService;
import com.bbmovie.transcodeworker.service.ladder.LadderGenerationService;
import com.bbmovie.transcodeworker.service.pipeline.dto.ExecuteTask;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeResult;
import com.bbmovie.transcodeworker.service.quality.dto.QualityReport;
import com.bbmovie.transcodeworker.service.storage.MinioUploadService;
import com.bbmovie.transcodeworker.service.validation.encode.dto.EncodingExpectations;
import com.bbmovie.transcodeworker.service.validation.encode.dto.ValidationReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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

    @Value("${app.analysis.vvs.fail-on-error:false}")
    private boolean failOnVvs;

    private final ValidationService validationService;
    private final VideoTranscoderService videoTranscoderService;
    private final LadderGenerationService ladderGenerationService;
    private final MinioUploadService uploadService;
    private final ComplexityAnalysisPort complexityAnalysisPort;
    private final EncodeValidationPort encodeValidationPort;
    private final VideoQualityPort videoQualityPort;
    private final AnalysisPersistenceService analysisPersistenceService;
    private final AnalysisEventPublisher analysisEventPublisher;

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

            // 3. CAS + determine resolutions
            ComplexityProfile complexityProfile = complexityAnalysisPort.analyze(taskId, metadata);
            analysisPersistenceService.saveComplexityProfile(complexityProfile);
            List<VideoTranscoderService.VideoResolution> resolutions =
                    ladderGenerationService.resolveEncodingLadder(
                            probe.targetResolutions(),
                            metadata,
                            complexityProfile.recipeHints()
                    );

            // 4. Transcode to HLS
            Path hlsOutputDir = outputDir.resolve("hls");
            videoTranscoderService.transcode(
                    inputFile,
                    resolutions,
                    hlsOutputDir.toString(),
                    taskId,
                    metadata
            );

            // 5. VVS + VQS on generated renditions
            runPostEncodeAnalysis(task, inputFile, hlsOutputDir, resolutions);

            // 6. Upload results to MinIO
            uploadHlsOutput(task, hlsOutputDir);

            log.info("Video processing completed: {} (video duration: {}s)", taskId, probe.duration());
            return ProcessingResult.success(probe.duration(), hlsOutputDir.toString());

        } catch (Exception e) {
            log.error("Video processing failed: {}", taskId, e);
            return ProcessingResult.failure(e.getMessage());
        }
    }

    private void runPostEncodeAnalysis(
            ExecuteTask task,
            Path sourceFile,
            Path hlsOutputDir,
            List<VideoTranscoderService.VideoResolution> resolutions) {
        for (VideoTranscoderService.VideoResolution resolution : resolutions) {
            try {
                Path playlist = hlsOutputDir.resolve(resolution.filename()).resolve("playlist.m3u8");
                if (!Files.exists(playlist)) {
                    continue;
                }

                EncodingExpectations expectations = new EncodingExpectations(
                        "h264",
                        resolution.width(),
                        resolution.height(),
                        null,
                        null,
                        "aac"
                );

                ValidationReport validationReport = encodeValidationPort.validate(
                        task.uploadId(),
                        playlist,
                        resolution.filename(),
                        expectations
                );
                analysisPersistenceService.saveValidationReport(task.uploadId(), resolution.filename(), validationReport);
                HashMap<String, Object> vvsPayload = new HashMap<>();
                vvsPayload.put("rendition", resolution.filename());
                vvsPayload.put("status", validationReport.status().name());
                analysisEventPublisher.publish(task.uploadId(), "VVS_COMPLETED", vvsPayload);

                if (failOnVvs && validationReport.status() == ValidationReport.ValidationStatus.FAIL) {
                    throw new RuntimeException("VVS failed for " + resolution.filename() + ": " + validationReport.violations());
                }

                QualityReport qualityReport = videoQualityPort.score(
                        task.uploadId(),
                        sourceFile,
                        playlist,
                        resolution.filename()
                );
                analysisPersistenceService.saveQualityReport(task.uploadId(), qualityReport);
                HashMap<String, Object> vqsPayload = new HashMap<>();
                vqsPayload.put("rendition", resolution.filename());
                vqsPayload.put("metric", qualityReport.metric());
                vqsPayload.put("score", qualityReport.score());
                vqsPayload.put("computed", qualityReport.computed());
                analysisEventPublisher.publish(task.uploadId(), "VQS_COMPLETED", vqsPayload);
            } catch (Exception e) {
                log.warn("Post-encode analysis failed for rendition {}: {}", resolution.filename(), e.getMessage());
                if (failOnVvs) {
                    throw e;
                }
            }
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

