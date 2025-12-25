package com.bbmovie.transcodeworker.service.ffmpeg;

import com.bbmovie.transcodeworker.exception.FileUploadException;
import com.bbmovie.transcodeworker.service.ffmpeg.option.CodecOptions;
import com.bbmovie.transcodeworker.service.ffmpeg.option.PresetOptions;
import com.bbmovie.transcodeworker.service.scheduler.ResolutionCostCalculator;
import com.bbmovie.transcodeworker.service.scheduler.TranscodeScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service responsible for video transcoding operations.
 * <p>
 * This service handles the conversion of video files into multiple resolutions (HLS format).
 * It coordinates with:
 * - {@link HlsKeyService} for encryption key management
 * - {@link HlsPlaylistService} for playlist generation
 * - {@link TranscodeScheduler} for resource management
 * <p>
 * Refactored to follow the Single Responsibility Principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTranscoderService {

    private final FFmpeg ffmpeg;
    private final HlsKeyService hlsKeyService;
    private final HlsPlaylistService hlsPlaylistService;
    private final TranscodeScheduler scheduler;
    private final ResolutionCostCalculator costCalculator;

    /** Executor for parallel transcoding tasks */
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Record representing a video resolution configuration for transcoding.
     *
     * @param width    the target width of the resolution
     * @param height   the target height of the resolution
     * @param filename the name used for the output resolution (e.g., "720p", "1080p")
     */
    public record VideoResolution(int width, int height, String filename) {
        /**
         * Returns the name of the resolution (same as filename).
         */
        public String name() {
            return filename;
        }

        /**
         * Gets the cost weight for this resolution based on transcoding complexity.
         */
        public int getCostWeight(ResolutionCostCalculator calculator) {
            return calculator.calculateCost(this.filename);
        }
    }

    /**
     * Record defining parameters for video resolution presets.
     */
    public record ResolutionDefinition(int minWidth, int targetWidth, int targetHeight, String suffix) {}

    /** Predefined resolution definitions */
    public static final List<ResolutionDefinition> PREDEFINED_RESOLUTIONS = List.of(
            new ResolutionDefinition(1920, 1920, 1080, "1080p"),
            new ResolutionDefinition(1280, 1280, 720, "720p"),
            new ResolutionDefinition(854, 854, 480, "480p"),
            new ResolutionDefinition(640, 640, 360, "360p"),
            new ResolutionDefinition(426, 426, 240, "240p"),
            new ResolutionDefinition(256, 256, 144, "144p")
    );

    /**
     * Determines the target resolutions based on input video metadata.
     *
     * @param meta the metadata of the input video
     * @return list of VideoResolution objects to generate
     */
    public List<VideoResolution> determineTargetResolutions(FFmpegVideoMetadata meta) {
        List<VideoResolution> targets = new ArrayList<>();
        for (ResolutionDefinition def : PREDEFINED_RESOLUTIONS) {
            if (meta.width() >= def.minWidth()) {
                targets.add(new VideoResolution(def.targetWidth(), def.targetHeight(), def.suffix()));
            }
        }
        if (targets.isEmpty()) {
            targets.add(new VideoResolution(meta.width(), meta.height(), "original"));
        }
        return targets;
    }

    /**
     * Performs the complete video transcoding process for multiple resolutions.
     * <p>
     * Resolutions are processed in parallel with resource management to prevent CPU overload.
     *
     * @param input            the path to the input video file
     * @param videoResolutions the list of resolutions to generate
     * @param outputDir        the directory where transcoded files should be stored
     * @param uploadId         the unique identifier for the upload operation
     * @param meta             the video metadata (pre-probed)
     */
    public void transcode(Path input, List<VideoResolution> videoResolutions, String outputDir, 
                          String uploadId, FFmpegVideoMetadata meta) {
        Path outputDirPath = Paths.get(outputDir);

        // Generate a master encryption key pair
        HlsKeyService.MasterKeyPair masterKeyPair = hlsKeyService.generateMasterKeyPair();

        log.info("Starting parallel transcoding for {} resolutions with resource management", videoResolutions.size());

        // Process all resolutions in parallel with resource constraints
        List<CompletableFuture<Void>> futures = videoResolutions.stream()
                .map(res -> CompletableFuture.runAsync(() -> 
                        transcodeResolution(input, res, outputDirPath, meta, uploadId, masterKeyPair), 
                        executorService))
                .toList();

        // Wait for all transcoding tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("All resolutions transcoded successfully, creating master playlist");
        hlsPlaylistService.createMasterPlaylist(videoResolutions, outputDirPath);
    }

    /**
     * Transcode a single resolution with resource management.
     */
    private void transcodeResolution(Path input, VideoResolution res, Path outputDir,
                                     FFmpegVideoMetadata meta, String uploadId,
                                     HlsKeyService.MasterKeyPair masterKeyPair) {
        TranscodeScheduler.ResourceHandle handle = null;
        try {
            int costWeight = res.getCostWeight(costCalculator);
            handle = scheduler.acquire(costWeight);
            int threadsToUse = handle.getActualThreads();

            log.info("[{}] Starting transcoding (cost: {}, threads: {}, maxCapacity: {})",
                    res.name(), costWeight, threadsToUse, scheduler.getMaxCapacity());

            executeHlsTranscode(input, res, outputDir, meta, uploadId, masterKeyPair, threadsToUse);

            log.info("[{}] Completed transcoding successfully", res.name());

        } catch (Exception e) {
            log.error("[{}] Failed to transcode resolution: {}", res.name(), e.getMessage(), e);
            throw new RuntimeException("Transcoding failed for resolution " + res.filename(), e);
        } finally {
            if (handle != null) {
                scheduler.release(handle);
            }
        }
    }

    /**
     * Executes the HLS transcode job for a specific video resolution.
     */
    private void executeHlsTranscode(Path input, VideoResolution res, Path outputDir,
                                     FFmpegVideoMetadata meta, String uploadId,
                                     HlsKeyService.MasterKeyPair masterKeyPair, int threadsToUse) {
        try {
            // Create a resolution directory
            Path resolutionDir = outputDir.resolve(res.name());
            Files.createDirectories(resolutionDir);

            Path playlistPath = resolutionDir.resolve("playlist.m3u8");
            String segmentFilename = resolutionDir.resolve("seg_%03d.ts").toString();

            // Calculate estimated segments
            int estimatedSegments = Math.max(1, (int) Math.ceil(meta.duration() / 10.0));
            log.debug("[{}] Estimated segments: {}", res.name(), estimatedSegments);

            // Generate encryption keys
            List<HlsKeyService.KeyInfo> keyInfos = hlsKeyService.generateKeyFiles(
                    resolutionDir, masterKeyPair.key(), masterKeyPair.iv(), estimatedSegments);
            log.info("[{}] Generated {} key files", res.name(), keyInfos.size());

            // Create FFmpeg key info file
            Path keyInfoPath = resolutionDir.resolve("keyinfo.txt");
            hlsKeyService.createKeyInfoFile(keyInfoPath, keyInfos, uploadId, res.name());

            // Build and execute FFmpeg command
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(input.toString())
                    .overrideOutputFiles(true)
                    .addOutput(playlistPath.toString())
                    .setVideoCodec(CodecOptions.libx264)
                    .setPreset(PresetOptions.veryfast)
                    .setAudioCodec("aac")
                    .setAudioBitRate(128000)
                    .setVideoFilter("scale=" + res.width() + ":-2")
                    .setFormat("hls")
                    .addExtraArgs("-hls_time", "10")
                    .addExtraArgs("-hls_list_size", "0")
                    .addExtraArgs("-hls_segment_filename", segmentFilename)
                    .addExtraArgs("-hls_playlist_type", "vod")
                    .addExtraArgs("-threads", String.valueOf(threadsToUse))
                    .addExtraArgs("-hls_key_info_file", keyInfoPath.toString())
                    .done();

            log.debug("[{}] FFmpeg configured with {} threads", res.name(), threadsToUse);

            FFmpegJob job = executor.createJob(builder, progress -> {
                if (meta.duration() > 0) {
                    double duration_ns = meta.duration() * 1_000_000_000.0;
                    double percentage = progress.out_time_ns / duration_ns;
                    log.debug("[{}] Transcoding: {}%", res.name(), String.format("%.2f", percentage * 100));
                }
            });
            job.run();

            // Cleanup temp key info file
            hlsKeyService.deleteKeyInfoFile(keyInfoPath);

            // Log generated files
            hlsPlaylistService.logGeneratedFiles(resolutionDir);

            // Update playlist URLs
            hlsPlaylistService.updatePlaylistUrls(playlistPath, uploadId, res.name(), keyInfos.size());

        } catch (IOException e) {
            log.error("[{}] Failed to execute HLS transcode: {}", res.name(), e.getMessage(), e);
            throw new FileUploadException("Unable to process file for resolution " + res.name());
        }
    }
}
